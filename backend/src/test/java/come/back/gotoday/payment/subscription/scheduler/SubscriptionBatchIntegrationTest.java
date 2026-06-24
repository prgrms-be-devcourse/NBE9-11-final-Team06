package come.back.gotoday.payment.subscription.scheduler;

import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.entity.OAuthProvider;
import come.back.gotoday.payment.billing.entity.BillingInfo;
import come.back.gotoday.payment.plan.entity.Plan;
import come.back.gotoday.payment.subscription.entity.PaymentHistory;
import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.PaymentStatus;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.subscription.repository.PaymentHistoryRepository;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(properties = {
        "KMA_WEATHER_API_KEY=mock_api_key",
        "weather.kma.service-key=mock_api_key",
        "SEOUL_CROWD_AREA_NAMES=강남역,홍대입구역"
})
@Slf4j
public class SubscriptionBatchIntegrationTest {

    @Autowired
    private SubscriptionBatchScheduler subscriptionBatchScheduler;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private come.back.gotoday.member.repository.MemberRepository memberRepository;

    @Autowired
    private come.back.gotoday.payment.plan.repository.PlanRepository planRepository;

    @Autowired
    private come.back.gotoday.payment.billing.repository.BillingInfoRepository billingInfoRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private TossPaymentsClient tossPaymentsClient;

    private Member testMember;
    private Plan testPlan;
    private BillingInfo testBillingInfo;

    @BeforeEach
    void setUp() {
        // [사전 작업] 안전하게 가짜 데이터를 부모 테이블부터 세팅합니다. (JPA Repository 사용)
        testMember = memberRepository.findByEmail("test@gotoday.com")
                .orElseGet(() -> memberRepository.save(
                        Member.createOAuthMember("test@gotoday.com", "테스트유저", "http://image.png", OAuthProvider.KAKAO, "kakao_1234", "ROLE_USER", "ACTIVE")
                ));

        testPlan = planRepository.findPlanByName("PREMIUM_PLAN")
                .orElseGet(() -> planRepository.save(
                        Plan.create("PREMIUM_PLAN", "프리미엄 멤버십", 14900L)
                ));

        testBillingInfo = billingInfoRepository.findByCustomerKey("cust_key_123")
                .orElseGet(() -> billingInfoRepository.save(
                        BillingInfo.create(testMember, "cust_key_123", "tos_billing_key_encrypted", "신한카드", "1234-5678-****-****")
                ));
    }

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @AfterEach
    void tearDown() {
        org.springframework.transaction.TransactionStatus status =
                transactionManager.getTransaction(new org.springframework.transaction.support.DefaultTransactionDefinition());

        try {
            //  자식 테이블부터 외래키 제약조건 순서에 맞춰 완벽하게 딜리트 진행
            paymentHistoryRepository.deleteAllInBatch();
            subscriptionRepository.deleteAllInBatch();

            billingInfoRepository.deleteAllInBatch();

            // 레포지토리가 없는 테이블들은 JPQL 벌크 연산으로 직접 제거
            entityManager.createQuery("delete from IdempotencyKey").executeUpdate();
            entityManager.createQuery("delete from RefreshToken").executeUpdate();
            entityManager.createQuery("delete from UserPreferenceCategory").executeUpdate();
            entityManager.createQuery("delete from UserPreference").executeUpdate();

            //  마지막에 마스터(부모) 테이블 삭제
            planRepository.deleteAllInBatch();
            memberRepository.deleteAllInBatch();

            entityManager.flush();
            entityManager.clear();

            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }

    private void forceSetNextBillingDate(Subscription subscription, LocalDate date) throws Exception {
        Field field = Subscription.class.getDeclaredField("nextBillingDate");
        field.setAccessible(true);
        field.set(subscription, date);
    }

    private TossAutomatedPaymentResponse createMockTossResponse(String paymentKey, String status) {
        return new TossAutomatedPaymentResponse(
                "tos-mid-123", "tx_key_0000", paymentKey, "ORD-BATCH-TEST", "프리미엄 멤버십",
                status, "2026-06-24T12:00:00+09:00", "2026-06-24T12:05:00+09:00", "BILLING", "카드", 14900L,
                new TossAutomatedPaymentResponse.CardInfo("SHINHAN", "SHINHAN", "1234-5678-****-****", 14900L, "CREDIT", "PERSONAL", "12345678"),
                new TossAutomatedPaymentResponse.ReceiptInfo("http://receipt.url/123")
        );
    }

    @Test
    @DisplayName("시나리오 1: 정상 정기 결제 성공 시 다음 결제일이 연장되고 성공 이력이 적재된다")
    @Transactional
    void automatedBilling_Success() throws Exception {
        // ==========================================
        // 1. Given (정산 대상 데이터 준비)
        // ==========================================
        LocalDate today = LocalDate.now();

        // 오늘 정산 대상이 될 구독 정보를 생성하고 활성화합니다.
        Subscription subscription = Subscription.startSubscription(testBillingInfo, testPlan, 14900L, today);
        subscription.activate();

        // Reflection을 사용해 결제 예정일을 '오늘'로 강제 패치 (배치 돌렸을 때 탐색되도록)
        forceSetNextBillingDate(subscription, today);
        subscriptionRepository.save(subscription);

        // 외부 토스 API가 정상적으로 'DONE'(성공)을 반환하도록 모킹(Mocking)
        TossAutomatedPaymentResponse mockResponse = new TossAutomatedPaymentResponse(
                "tos-mid-123", "tx_key_0000", "pay_key_abcdef", "ORD-BATCH-TEST", "프리미엄 멤버십",
                "DONE", "2026-06-24T12:00:00+09:00", "2026-06-24T12:05:00+09:00", "BILLING", "카드", 14900L,
                new TossAutomatedPaymentResponse.CardInfo("SHINHAN", "SHINHAN", "1234-5678-****-****", 14900L, "CREDIT", "PERSONAL", "12345678"),
                new TossAutomatedPaymentResponse.ReceiptInfo("http://receipt.url/123")
        );
        Mockito.when(tossPaymentsClient.requestPayment(eq("tos_billing_key_encrypted"), any())).thenReturn(mockResponse);

        // ==========================================
        // 2. When (배치 스케줄러 가동)
        // ==========================================
        subscriptionBatchScheduler.runAutomatedBillingPayment();

        // ==========================================
        // 3. Then (체크포인트 검증)
        // ==========================================

        // [체크 1] 외부 결제 API(토스)가 정상적으로 호출되었는가?
        Mockito.verify(tossPaymentsClient, Mockito.times(1)).requestPayment(eq("tos_billing_key_encrypted"), any());

        // 영속성 컨텍스트를 반영하여 최신 DB 데이터를 다시 조회
        Subscription updatedSubscription = subscriptionRepository.findById(subscription.getId()).orElseThrow();

        // [체크 2] Subscription의 상태가 ACTIVE(활성화)로 유지되는가?
        assertThat(updatedSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        // [체크 3] 차기 결제일(nextBillingDate)이 오늘 기준으로 '정확히 1달 뒤'로 연장되었는가?
        assertThat(updatedSubscription.getNextBillingDate()).isEqualTo(today.plusMonths(1));

        // [체크 4] PaymentHistory(결제 이력)에 SUCCESS(성공) 데이터가 정상 적재되었는가?
        List<PaymentHistory> histories = paymentHistoryRepository.findAll();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(histories.get(0).getAmount()).isEqualTo(14900L);
    }

    @Test
    @DisplayName("시나리오 1-1: 정산 대상이 여러 건(3건)일 때, 배치가 루프를 돌며 모두 각각 결제에 성공하고 이력을 남긴다")
    @Transactional
    void automatedBilling_MultipleSuccess() throws Exception {
        // ==========================================
        // 1. Given (정산 대상 데이터 '3건' 준비)
        // ==========================================
        LocalDate today = LocalDate.now();

        // [대상 1] 기존에 setUp에서 만든 testMember용 구독
        Subscription sub1 = Subscription.startSubscription(testBillingInfo, testPlan, 14900L, today);
        sub1.activate();
        forceSetNextBillingDate(sub1, today);
        subscriptionRepository.save(sub1);

        // [대상 2] 또 다른 회원과 빌링키 생성 후 구독 추가
        Member member2 = memberRepository.save(Member.createOAuthMember("test2@gotoday.com", "유저2", "img", OAuthProvider.KAKAO, "kakao_2", "ROLE_USER", "ACTIVE"));
        BillingInfo billing2 = billingInfoRepository.save(BillingInfo.create(member2, "cust_key_456", "billing_key_2_encrypted", "국민카드", "4321-****"));
        Subscription sub2 = Subscription.startSubscription(billing2, testPlan, 14900L, today);
        sub2.activate();
        forceSetNextBillingDate(sub2, today);
        subscriptionRepository.save(sub2);

        // [대상 3] 세 번째 회원과 빌링키 생성 후 구독 추가
        Member member3 = memberRepository.save(Member.createOAuthMember("test3@gotoday.com", "유저3", "img", OAuthProvider.KAKAO, "kakao_3", "ROLE_USER", "ACTIVE"));
        BillingInfo billing3 = billingInfoRepository.save(BillingInfo.create(member3, "cust_key_789", "billing_key_3_encrypted", "삼성카드", "5555-****"));
        Subscription sub3 = Subscription.startSubscription(billing3, testPlan, 14900L, today);
        sub3.activate();
        forceSetNextBillingDate(sub3, today);
        subscriptionRepository.save(sub3);

        // Mock 객체 정의: 어떤 빌링키가 들어오든 무조건 성공 응답('DONE')을 반환하도록 설정
        TossAutomatedPaymentResponse mockResponse = new TossAutomatedPaymentResponse(
                "tos-mid-123", "tx_key_mock", "pay_key_mock", "ORD-BATCH-TEST", "프리미엄 멤버십",
                "DONE", "2026-06-24T12:00:00+09:00", "2026-06-24T12:05:00+09:00", "BILLING", "카드", 14900L,
                new TossAutomatedPaymentResponse.CardInfo("CARD", "CARD", "****-****-****-****", 14900L, "CREDIT", "PERSONAL", "12345678"),
                new TossAutomatedPaymentResponse.ReceiptInfo("http://receipt.url")
        );
        // any()를 사용하여 어떤 빌링키 요청이든 mockResponse를 반환하게 엽니다.
        Mockito.when(tossPaymentsClient.requestPayment(any(), any())).thenAnswer(invocation -> {
            // 호출될 때마다 랜덤한 UUID를 붙여서 중복을 원천 차단합니다.
            String uniqueKey = "pay_key_" + java.util.UUID.randomUUID().toString().substring(0, 8);
            String uniqueOrderId = "ORD-BATCH-" + java.util.UUID.randomUUID().toString().substring(0, 8);

            return new TossAutomatedPaymentResponse(
                    "tos-mid-123", "tx_key_mock", uniqueKey, uniqueOrderId, "프리미엄 멤버십",
                    "DONE", "2026-06-24T12:00:00+09:00", "2026-06-24T12:05:00+09:00", "BILLING", "카드", 14900L,
                    new TossAutomatedPaymentResponse.CardInfo("CARD", "CARD", "****-****-****-****", 14900L, "CREDIT", "PERSONAL", "12345678"),
                    new TossAutomatedPaymentResponse.ReceiptInfo("http://receipt.url")
            );
        });

        // ==========================================
        // 2. When (배치 가동 - 3건 한 번에 처리)
        // ==========================================
        subscriptionBatchScheduler.runAutomatedBillingPayment();

        // ==========================================
        // 3. Then (체크포인트 검증)
        // ==========================================

        // [체크 1] 대상이 3명이므로 토스 API 호출이 정확히 '3번' 일어났는가?
        // 각 회원의 암호화된 빌링키가 인자로 정상 전달되었는지 개별 검증도 가능합니다.
        Mockito.verify(tossPaymentsClient, Mockito.times(3)).requestPayment(any(), any());

        // 최신 DB 상태 조회
        Subscription updatedSub1 = subscriptionRepository.findById(sub1.getId()).orElseThrow();
        Subscription updatedSub2 = subscriptionRepository.findById(sub2.getId()).orElseThrow();
        Subscription updatedSub3 = subscriptionRepository.findById(sub3.getId()).orElseThrow();

        // [체크 2] 3개 구독 정보 모두 ACTIVE 상태를 유지하는가?
        assertThat(updatedSub1.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updatedSub2.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updatedSub3.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        // [체크 3] 3개 구독 정보 모두 차기 결제일이 한 달 뒤로 정확히 연장되었는가?
        assertThat(updatedSub1.getNextBillingDate()).isEqualTo(today.plusMonths(1));
        assertThat(updatedSub2.getNextBillingDate()).isEqualTo(today.plusMonths(1));
        assertThat(updatedSub3.getNextBillingDate()).isEqualTo(today.plusMonths(1));

        // [체크 4] 결제 이력(PaymentHistory)에 총 3건의 SUCCESS 데이터가 적재되었는가?
        List<PaymentHistory> histories = paymentHistoryRepository.findAll();
        assertThat(histories).hasSize(3);
        assertThat(histories).allMatch(history -> history.getStatus() == PaymentStatus.SUCCESS);
    }
    @Test
    @DisplayName("시나리오 2-1: 결제 중 외부 API 예외(잔액 부족 등) 발생 시, 구독은 유예 상태가 되고 실패 이력이 적재된다")
    @Transactional
    void automatedBilling_Failure_To_Pending() throws Exception {
        // ==========================================
        // 1. Given (정산 대상 데이터 준비)
        // ==========================================
        LocalDate today = LocalDate.now();

        Subscription subscription = Subscription.startSubscription(testBillingInfo, testPlan, 14900L, today);
        subscription.activate();
        forceSetNextBillingDate(subscription, today);
        subscriptionRepository.save(subscription);

        // 아주 긴 예외 메시지 발생 시뮬레이션 (Data Truncation 방어 테스트 포함)
        String longErrorMessage = "Toss Payments API Error: 신용카드 한도 초과로 인해 결제 승인이 거절되었습니다. "
                + "Detail: [CardErrorCode=EXCEED_MAX_DAILY_PAYMENT_LIMIT] "
                + "Stacktrace: " + "a".repeat(300); // 300자가 넘는 에러 메시지 생성

        // 토스 클라이언트가 결제 요청 시 런타임 예외를 던지도록 모킹
        Mockito.when(tossPaymentsClient.requestPayment(any(), any()))
                .thenThrow(new RuntimeException(longErrorMessage));

        // ==========================================
        // 2. When (배치 스케줄러 가동)
        // ==========================================
        // 내부적으로 try-catch가 잘 되어 있다면 배치 메서드 자체가 에러로 뻗지 않고 정상 종료되어야 합니다.
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            subscriptionBatchScheduler.runAutomatedBillingPayment();
        });

        // ==========================================
        // 3. Then (결과 검증)
        // ==========================================

        // 최신 구독 정보 조회
        Subscription updatedSubscription = subscriptionRepository.findById(subscription.getId()).orElseThrow();

        // [체크 1] 구독 상태가 활성(ACTIVE)에서 결제 유예(EXPIRED_PAYMENT_PENDING)로 변경되었는가?
        assertThat(updatedSubscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED_PAYMENT_PENDING);

        // [체크 2] 실패했으므로 차기 결제일은 연장되지 않고 오늘 그대로여야 함
        assertThat(updatedSubscription.getNextBillingDate()).isEqualTo(today);

        // [체크 3] 결제 이력(PaymentHistory)에 FAILED 데이터가 적재되었는가?
        List<PaymentHistory> histories = paymentHistoryRepository.findAll();
        assertThat(histories).hasSize(1);

        PaymentHistory failureHistory = histories.get(0);
        assertThat(failureHistory.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failureHistory.getAmount()).isEqualTo(14900L);

        // [체크 4] 방어 코드가 작동하여 failure_reason이 DB 용량을 초과하지 않고 잘려서 저장되었는가?
        assertThat(failureHistory.getFailureReason()).isNotNull();
        assertThat(failureHistory.getFailureReason().length()).isLessThanOrEqualTo(255);
        log.info("저장된 실패 사유 요약: {}", failureHistory.getFailureReason());
    }


    @Test
    @DisplayName("시나리오 2-2: 다건 정산 중 특정 건이 실패하더라도 다른 건의 결제 성공 및 데이터 반영에는 영향을 주지 않는다")
    @Transactional
    void automatedBilling_MixedResult() throws Exception {
        // ==========================================
        // 1. Given (정산 대상 2건 준비)
        // ==========================================
        LocalDate today = LocalDate.now();

        // [유저 1] 결제에 성공할 유저
        Subscription successSub = Subscription.startSubscription(testBillingInfo, testPlan, 14900L, today);
        successSub.activate();
        forceSetNextBillingDate(successSub, today);
        subscriptionRepository.save(successSub);

        // [유저 2] 잔액 부족으로 실패할 유저
        Member member2 = memberRepository.save(Member.createOAuthMember("failuser@gotoday.com", "실패유저", "img", OAuthProvider.KAKAO, "kakao_fail", "ROLE_USER", "ACTIVE"));
        BillingInfo billing2 = billingInfoRepository.save(BillingInfo.create(member2, "cust_key_fail", "billing_key_fail_encrypted", "국민카드", "4321-****"));
        Subscription failSub = Subscription.startSubscription(billing2, testPlan, 14900L, today);
        failSub.activate();
        forceSetNextBillingDate(failSub, today);
        subscriptionRepository.save(failSub);

        // Mocking: 빌링키가 'billing_key_fail_encrypted' 이면 예외 발생, 그 외에는 정상 응답 동적 반환
        Mockito.when(tossPaymentsClient.requestPayment(any(), any())).thenAnswer(invocation -> {
            String billingKey = invocation.getArgument(0);

            if ("billing_key_fail_encrypted".equals(billingKey)) {
                throw new RuntimeException("잔액 부족으로 인한 승인 거절");
            }

            return new TossAutomatedPaymentResponse(
                    "tos-mid-123", "tx_key_ok", "pay_key_ok_" + java.util.UUID.randomUUID().toString().substring(0,4),
                    "ORD-" + java.util.UUID.randomUUID().toString().substring(0,4), "프리미엄 멤버십",
                    "DONE", "2026-06-24T12:00:00+09:00", "2026-06-24T12:05:00+09:00", "BILLING", "카드", 14900L,
                    new TossAutomatedPaymentResponse.CardInfo("CARD", "CARD", "****-****-****-****", 14900L, "CREDIT", "PERSONAL", "12345678"),
                    new TossAutomatedPaymentResponse.ReceiptInfo("http://receipt.url")
            );
        });

        // ==========================================
        // 2. When (배치 스케줄러 가동)
        // ==========================================
        subscriptionBatchScheduler.runAutomatedBillingPayment();

        // ==========================================
        // 3. Then (각각 독립적으로 반영되었는지 검증)
        // ==========================================

        // 1번 구독 검증: 성공했으므로 ACTIVE 유지 + 차기 결제일 한 달 뒤로 연장
        Subscription updatedSuccessSub = subscriptionRepository.findById(successSub.getId()).orElseThrow();
        assertThat(updatedSuccessSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updatedSuccessSub.getNextBillingDate()).isEqualTo(today.plusMonths(1));

        // 2번 구독 검증: 실패했으므로 EXPIRED_PAYMENT_PENDING 변경 + 차기 결제일 유지
        Subscription updatedFailSub = subscriptionRepository.findById(failSub.getId()).orElseThrow();
        assertThat(updatedFailSub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED_PAYMENT_PENDING);
        assertThat(updatedFailSub.getNextBillingDate()).isEqualTo(today);

        // 결제 이력 총 2건 (성공 1건, 실패 1건) 정상 적재 검증
        List<PaymentHistory> histories = paymentHistoryRepository.findAll();
        assertThat(histories).hasSize(2);

        long successCount = histories.stream().filter(h -> h.getStatus() == PaymentStatus.SUCCESS).count();
        long failCount = histories.stream().filter(h -> h.getStatus() == PaymentStatus.FAILED).count();

        assertThat(successCount).isEqualTo(1L); // 성공 이력은 정확히 1건이어야 함
        assertThat(failCount).isEqualTo(1L);    // 실패 이력도 정확히 1건이어야 함
    }

    @Test
    @DisplayName("시나리오 3-1: 최초 결제 실패 후 3일밖에 지나지 않은 구독은 해지되지 않고 결제를 재시도하여 활성화된다")
    @Transactional
    void automatedBilling_GracePeriod_NotExpired_Should_RetryPayment() throws Exception {
        // ==========================================
        // 1. Given (3일 전 실패한 유예 상태의 구독 준비)
        // ==========================================
        LocalDate today = LocalDate.now();

        Subscription keepingSub = Subscription.startSubscription(testBillingInfo, testPlan, 14900L, today);
        keepingSub.changeToPaymentBatchFail(today.minusDays(3)); // 3일 전 실패 (유예 기간 내)
        forceSetNextBillingDate(keepingSub, today);
        subscriptionRepository.save(keepingSub);

        // 토스 API가 결제 재시도 시 정상 응답을 주도록 설정
        TossAutomatedPaymentResponse mockResponse = new TossAutomatedPaymentResponse(
                "tos-mid-123", "tx_key_ok", "pay_key_retry", "ORD-RETRY", "프리미엄 멤버십",
                "DONE", "2026-06-24T12:00:00+09:00", "2026-06-24T12:05:00+09:00", "BILLING", "카드", 14900L,
                new TossAutomatedPaymentResponse.CardInfo("CARD", "CARD", "****-****", 14900L, "CREDIT", "PERSONAL", "1234"),
                new TossAutomatedPaymentResponse.ReceiptInfo("http://receipt.url")
        );
        Mockito.when(tossPaymentsClient.requestPayment(any(), any())).thenReturn(mockResponse);

        // ==========================================
        // 2. When (배치 가동)
        // ==========================================
        subscriptionBatchScheduler.runAutomatedBillingPayment();

        // ==========================================
        // 3. Then (검증)
        // ==========================================
        Subscription updatedSub = subscriptionRepository.findById(keepingSub.getId()).orElseThrow();

        // [체크] 아직 기회가 있으므로 ACTIVE 상태로 복구되고, 결제일이 1달 뒤로 연장되어야 함
        assertThat(updatedSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updatedSub.getNextBillingDate()).isEqualTo(today.plusMonths(1));

        // [체크] 토스 API 결제 요청이 정확히 '1번' 호출되었어야 함
        Mockito.verify(tossPaymentsClient, Mockito.times(1)).requestPayment(eq("tos_billing_key_encrypted"), any());
    }

    @Test
    @DisplayName("시나리오 3-2: 유예 기간(7일)을 초과한 구독은 토스 API를 호출하지 않고 즉시 CANCELED 상태로 해지된다")
    @Transactional
    void automatedBilling_GracePeriod_Expired_Should_Cancel_Without_ApiCall() throws Exception {
        // ==========================================
        // 1. Given (8일 전 실패하여 유예 기간이 끝난 구독 준비)
        // ==========================================
        LocalDate today = LocalDate.now();

        Subscription expiredSub = Subscription.startSubscription(testBillingInfo, testPlan, 14900L, today);
        expiredSub.changeToPaymentBatchFail(today.minusDays(8)); // 8일 전 실패 (유예 기간 만료)
        forceSetNextBillingDate(expiredSub, today);
        subscriptionRepository.save(expiredSub);

        // ==========================================
        // 2. When (배치 가동)
        // ==========================================
        subscriptionBatchScheduler.runAutomatedBillingPayment();

        // ==========================================
        // 3. Then (검증)
        // ==========================================
        Subscription updatedSub = subscriptionRepository.findById(expiredSub.getId()).orElseThrow();

        // [체크] 최종 해지 상태인 CANCELED로 안전하게 변경되었는가?
        assertThat(updatedSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);

        // [체크] ★핵심★ 이미 만료된 유저이므로 외부 토스 결제 API(requestPayment) 호출 횟수는 정확히 '0회'여야 함
        Mockito.verify(tossPaymentsClient, Mockito.times(0)).requestPayment(any(), any());
    }

    @Test
    @DisplayName("시나리오 3-3: 유예 만료로 해지될 때 '유예 기간 만료' 사유가 적힌 PaymentHistory 실패 이력이 적재된다")
    @Transactional
    void automatedBilling_GracePeriod_Expired_Should_Record_FailureHistory() throws Exception {
        // ==========================================
        // 1. Given (유예 기간이 만료된 구독 준비)
        // ==========================================
        LocalDate today = LocalDate.now();

        Subscription expiredSub = Subscription.startSubscription(testBillingInfo, testPlan, 14900L, today);
        expiredSub.changeToPaymentBatchFail(today.minusDays(10)); // 10일 전 실패
        forceSetNextBillingDate(expiredSub, today);
        subscriptionRepository.save(expiredSub);

        // ==========================================
        // 2. When (배치 가동)
        // ==========================================
        subscriptionBatchScheduler.runAutomatedBillingPayment();

        // ==========================================
        // 3. Then (검증)
        // ==========================================
        List<PaymentHistory> histories = paymentHistoryRepository.findAll();

        // [체크] 결제 승인은 안 갔지만, 강제 해지 이력 1건이 적재되어 있어야 함
        assertThat(histories).hasSize(1);

        PaymentHistory systemCancelHistory = histories.get(0);
        // [체크] 이력 상태가 FAILED(또는 정해둔 시스템 해지 규격)이고, 사유에 '만료'나 '해지'가 명시되어 있는지 검증
        assertThat(systemCancelHistory.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(systemCancelHistory.getFailureReason()).contains("유예 기간 초과");
    }


    @Test
    @DisplayName("시나리오 4-1: 배치가 결제 승인을 요청하기 직전 사용자가 먼저 결제를 성공시켰다면, 배치는 API 호출을 스킵한다")
    @Transactional
    void automatedBilling_Concurrency_UserDirectPayment_First() throws Exception {
        // ==========================================
        // 1. Given (오늘 정산 대상인 구독 데이터 세팅)
        // ==========================================
        LocalDate today = LocalDate.now();

        Subscription subscription = Subscription.startSubscription(testBillingInfo, testPlan, 14900L, today);
        subscription.activate();
        forceSetNextBillingDate(subscription, today);
        subscriptionRepository.saveAndFlush(subscription);

        // 토스 API 가짜 응답 세팅
        TossAutomatedPaymentResponse mockResponse = new TossAutomatedPaymentResponse(
                "tos-mid-123", "tx_key_user", "pay_key_user", "ORD-USER", "프리미엄 멤버십",
                "DONE", "2026-06-24T12:00:00+09:00", "2026-06-24T12:05:00+09:00", "BILLING", "카드", 14900L,
                new TossAutomatedPaymentResponse.CardInfo("CARD", "CARD", "****-****", 14900L, "CREDIT", "PERSONAL", "1234"),
                new TossAutomatedPaymentResponse.ReceiptInfo("http://receipt.url")
        );
        Mockito.when(tossPaymentsClient.requestPayment(any(), any())).thenReturn(mockResponse);

        // ==========================================
        // 2. When (배치가 돌기 직전, 다른 스레드가 먼저 결제를 성공시킴)
        // ==========================================
        // 💡 [시뮬레이션] 사용자가 직접 결제하여 '차기 결제일'을 먼저 한 달 뒤로 업데이트해 버림
        // 실제 운영 환경에서는 비관적 락에 의해 선행 스레드가 이 작업을 완료하고 트랜잭션을 커밋한 상태입니다.
        subscription.renewNextBillingDate(); // nextBillingDate를 오늘 -> 한 달 뒤로 강제 연장
        subscriptionRepository.saveAndFlush(subscription);

        // 그 상태에서 배치가 가동됩니다.
        subscriptionBatchScheduler.runAutomatedBillingPayment();

        // ==========================================
        // 3. Then (더블 체킹 스킵 검증)
        // ==========================================

        // [체크] 이미 사용자가 결제를 끝냈으므로, 배치는 외부 토스 API를 "0회" 호출했어야 함 (스킵 방어 완료)
        Mockito.verify(tossPaymentsClient, Mockito.times(0)).requestPayment(any(), any());

        // 데이터가 이중 연장되어 두 달 뒤가 되지 않고, 정확히 한 달 뒤 상태를 유지해야 함
        Subscription updatedSub = subscriptionRepository.findById(subscription.getId()).orElseThrow();
        assertThat(updatedSub.getNextBillingDate()).isEqualTo(today.plusMonths(1));
    }

    @Test
    @DisplayName("시나리오 4-2: 동일한 정산 배치가 동시에 2개 가동되어 경합하더라도, 비관적 락으로 인해 토스 API는 1번만 호출된다")
    void automatedBilling_Concurrency_DuplicateBatch_Running() throws Exception {
        // ==========================================
        // 1. Given (정산 대상 데이터 1건 세팅)
        // ==========================================
        LocalDate today = LocalDate.now();

        org.springframework.transaction.TransactionStatus givenStatus =
                transactionManager.getTransaction(new org.springframework.transaction.support.DefaultTransactionDefinition());

        Subscription subscription;
        try {
            Subscription sub = Subscription.startSubscription(testBillingInfo, testPlan, 14900L, today);
            sub.activate();
            forceSetNextBillingDate(sub, today);
            subscription = subscriptionRepository.saveAndFlush(sub);

            transactionManager.commit(givenStatus); // DB에 확실하게 먼저 커밋 반영
        } catch (Exception e) {
            transactionManager.rollback(givenStatus);
            throw e;
        }

        // 토스 API 가짜 응답 및 150ms 인위적 딜레이 설정
        Mockito.when(tossPaymentsClient.requestPayment(any(), any())).thenAnswer(invocation -> {
            Thread.sleep(150);
            return new TossAutomatedPaymentResponse(
                    "tos-mid-123", "tx_key_dup", "pay_key_" + java.util.UUID.randomUUID().toString().substring(0,4),
                    "ORD-DUP", "프리미엄 멤버십", "DONE", "2026-06-24T12:00:00+09:00", "2026-06-24T12:05:00+09:00",
                    "BILLING", "카드", 14900L,
                    new TossAutomatedPaymentResponse.CardInfo("CARD", "CARD", "****-****", 14900L, "CREDIT", "PERSONAL", "1234"),
                    new TossAutomatedPaymentResponse.ReceiptInfo("http://receipt.url")
            );
        });

        // 멀티스레드(2개 스레드) 동시 경합 세팅
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // ==========================================
        // 2. When (수동 트랜잭션 껍데기를 벗겨내고 순수하게 가동)
        // ==========================================
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    // 💡 상위 트랜잭션 방해 없이, 서비스 로직 내부의 @Transactional들이
                    // 제 타이밍에 생성되고 제 타이밍에 커밋(락 해제 및 PROCESSING 상태 실시간 반영)되도록 만듭니다.
                    subscriptionBatchScheduler.runAutomatedBillingPayment();
                } catch (Exception e) {
                    log.error("배치 중복 가동 중 에러 발생: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // ==========================================
        // 3. Then (비관적 락 제어력 검증)
        // ==========================================

        // 비관적 락에 의해 줄을 섰고, 먼저 락을 풀고 나간 스레드가 PROCESSING 커밋을 완료했으므로
        // 뒤늦은 스레드는 더블체킹에 막혀 토스 API 호출 횟수는 무조건 "정확히 1번"이어야 함
        Mockito.verify(tossPaymentsClient, Mockito.times(1)).requestPayment(any(), any());

        // 최종 상태 조회를 위한 독립 트랜잭션 개시
        org.springframework.transaction.TransactionStatus thenStatus =
                transactionManager.getTransaction(new org.springframework.transaction.support.DefaultTransactionDefinition());

        Subscription updatedSub;
        List<PaymentHistory> histories;
        try {
            updatedSub = subscriptionRepository.findById(subscription.getId()).orElseThrow();
            histories = paymentHistoryRepository.findBySubscriptionId(subscription.getId());
            transactionManager.commit(thenStatus);
        } catch (Exception e) {
            transactionManager.rollback(thenStatus);
            throw e;
        }

        // 단언문 검증
        assertThat(updatedSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updatedSub.getNextBillingDate()).isEqualTo(today.plusMonths(1));
        assertThat(histories).hasSize(1);
    }
}