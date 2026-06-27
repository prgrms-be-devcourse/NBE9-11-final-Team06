package come.back.gotoday.payment.settlement.scheduler;

import come.back.gotoday.external.toss.dto.SettlementDto.TossSettlementResponse;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.payment.billing.entity.BillingInfo;
import come.back.gotoday.payment.billing.repository.BillingInfoRepository;
import come.back.gotoday.payment.plan.entity.Plan;
import come.back.gotoday.payment.plan.repository.PlanRepository;
import come.back.gotoday.payment.settlement.entity.SettlementDetail;
import come.back.gotoday.payment.settlement.enums.SettlementStatus;
import come.back.gotoday.payment.settlement.repository.SettlementDetailRepository;
import come.back.gotoday.payment.history.entity.PaymentHistory;
import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.history.repository.PaymentHistoryRepository;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "KMA_WEATHER_API_KEY=mock_api_key",
        "weather.kma.service-key=mock_api_key",
        "TOUR_API_KEY=mock_tour_api_key",
        "external.tour.api-key=mock_tour_api_key",
        "KAKAO_REST_API_KEY=mock_kakao_rest_api_key",
        "external.kakao.api-key=mock_kakao_rest_api_key",
        "SEOUL_CROWD_AREA_NAMES=강남역,홍대입구역",
        "crowd.scheduler.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = {"USER", "ADMIN"})
class SettlementControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private BillingInfoRepository billingInfoRepository;
    @Autowired private PlanRepository planRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private PaymentHistoryRepository paymentHistoryRepository;
    @Autowired private SettlementDetailRepository settlementDetailRepository;

    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        // 1. 제공된 실제 명세 규칙대로 Member 인스턴스 생성 및 영속화
        Member member = Member.create(
                "test-user@gotoday.com",
                "encrypted_password_1234",
                "테스트유저",
                "ROLE_USER",
                "ACTIVE"
        );
        Member savedMember = memberRepository.save(member);

        // 2. 제공된 실제 명세 규칙대로 BillingInfo 인스턴스 생성 및 영속화
        BillingInfo billingInfo = BillingInfo.create(
                savedMember,
                "customer_key_test_1234",
                "toss_billing_key_mock_512bytes_length_something...",
                "토스카드",
                "1234-56**-****-1234"
        );
        BillingInfo savedBilling = billingInfoRepository.save(billingInfo);

        // 3. 제공된 실제 명세 규칙대로 Plan 인스턴스 생성 및 영속화
        Plan savedPlan = planRepository.findPlanByName("PREMIUM_PLAN") // 혹은 findById 등 레포지토리 메서드 활용
                .orElseGet(() -> planRepository.save(Plan.create(
                        "PREMIUM_PLAN",
                        "프리미엄 멤버십",
                        99800L
                )));

        // 4. 제공된 실제 명세 규칙대로 Subscription 인스턴스 생성 및 활성화 처리
        LocalDate startDate = LocalDate.now();
        Subscription subscription = Subscription.startSubscription(
                savedBilling,
                savedPlan,
                99800L,
                startDate
        );

        // 정산 대조 후 도메인 상태 변경 로직이 원활하게 작동할 수 있도록 활성화 상태로 갱신
        subscription.activate();
        testSubscription = subscriptionRepository.save(subscription);

        // 데이터 정합성 대조를 위한 기반 외래키 데이터 쓰기 즉시 동기화
        subscriptionRepository.flush();
    }

    @Test
    @DisplayName("정상 결제 건의 정산 데이터를 주입하면 대조 성공 장부(MATCHED)가 완전히 적재된다.")
    void testReconcileSuccess_HappyPath() throws Exception {
        // ==========================================
        // 1. Given: 실제 우리 DB 상태 정의 (제공된 PaymentHistory 스펙 100% 준수)
        // ==========================================
        String targetOrderId = "EjBNtZK7j8q2TlGFLJ-9T";
        String targetPaymentKey = "xLpgeoO7410238740297423RBKEzMjPJyG";
        Long orderAmount = 99800L;

        // 제공해주신 PaymentHistory.createSuccessHistory 정적 팩토리 메서드 활용하여 실제 영속화
        PaymentHistory existingHistory = PaymentHistory.createSuccessHistory(
                testSubscription,
                targetPaymentKey,
                targetOrderId,
                orderAmount,
                "https://tosspayments.com/receipt/test"
        );
        paymentHistoryRepository.saveAndFlush(existingHistory);

        // ==========================================
        // 2. Given: 토스 응답 데이터 규격에 맞춘 가짜 요청 데이터 바인딩
        // ==========================================
        TossSettlementResponse mockTossResponse = new TossSettlementResponse(
                targetOrderId,
                targetPaymentKey,
                orderAmount,          // 우리 DB에 적재한 금액과 정확히 일치
                2250L,                // fee (수수료)
                205L,                 // vat (부가세)
                97345L,               // payOutAmount (실지급액)
                LocalDate.of(2023, 11, 30), // 제공해주신 토스 실제 JSON 응답 내 paidOutDate 반영
                null                  // 일반 성공 결제이므로 cancel 객체는 null
        );

        List<TossSettlementResponse> requestBodyList = List.of(mockTossResponse);
        String jsonContent = objectMapper.writeValueAsString(requestBodyList);

        // ==========================================
        // 3. When: 컨트롤러 테스트 엔드포인트 HTTP POST 요청 실제 수행
        // ==========================================
        mockMvc.perform(post("/api/settlement/test-reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk());

        // ==========================================
        // 4. Then: 데이터베이스 최종 적재 결과 장부 검증
        // ==========================================
        List<SettlementDetail> savedDetails = settlementDetailRepository.findAll();

        // 1건이 성공적으로 적재되었는지 확인
        assertThat(savedDetails).hasSize(1);
        SettlementDetail actualDetail = savedDetails.get(0);

        // 요청한 정산 필드값들이 누락 없이 장부에 잘 적입되었는지 대조 검증
        assertThat(actualDetail.getOrderId()).isEqualTo(targetOrderId);
        assertThat(actualDetail.getPaymentKey()).isEqualTo(targetPaymentKey);
        assertThat(actualDetail.getAmount()).isEqualTo(orderAmount);
        assertThat(actualDetail.getFee()).isEqualTo(2250L);
        assertThat(actualDetail.getVat()).isEqualTo(205L);
        assertThat(actualDetail.getPayOutAmount()).isEqualTo(97345L);
        assertThat(actualDetail.getSettlementDate()).isEqualTo(LocalDate.of(2023, 11, 30));

        // 비즈니스 정합성 지표 검증 (가장 중요한 포인트)
        assertThat(actualDetail.getStatus()).isEqualTo(SettlementStatus.MATCHED);
        assertThat(actualDetail.getPaymentHistory().getId()).isEqualTo(existingHistory.getId());
    }

    @Test
    @DisplayName("취소(환불) 건의 마이너스 정산 데이터 유입 시 대조 성공 장부(MATCHED)가 정상 적재된다.")
    void testReconcileSuccess_CancelMatched() throws Exception {
        // ==========================================
        // 1. Given: 실제 우리 DB 상태 정의 (결제 취소 상태 기록 선적)
        // ==========================================
        String targetOrderId = "EjBNtZK7j8q2TlGFLJ-9T";
        String targetPaymentKey = "xLpgeoO7410238740297423RBKEzMjPJyG";
        Long orderAmount = 99800L;

        // 원천 결제 성공 이력을 먼저 저장
        PaymentHistory existingHistory = PaymentHistory.createSuccessHistory(
                testSubscription,
                targetPaymentKey,
                targetOrderId,
                orderAmount,
                "https://tosspayments.com/receipt/test"
        );
        paymentHistoryRepository.saveAndFlush(existingHistory);

        // 비즈니스 흐름(구독 해지/취소)에 맞춰 우리 DB의 결제 상태를 CANCELED로 전이
        existingHistory.cancel();
        paymentHistoryRepository.saveAndFlush(existingHistory);

        // ==========================================
        // 2. Given: 제공해주신 SettlementDto.CancelInfo record 명세 구조 반영
        // ==========================================
        // 순서: cancelReason, cancelAmount, cancelStatus
        come.back.gotoday.external.toss.dto.SettlementDto.CancelInfo mockCancelInfo =
                new come.back.gotoday.external.toss.dto.SettlementDto.CancelInfo(
                        "고객 변심에 의한 구독 해지",
                        99800L,
                        "DONE"
                );

        // 토스페이먼츠 취소 정산 응답 DTO 구성 (금액 부호는 마이너스)
        TossSettlementResponse mockTossResponse = new TossSettlementResponse(
                targetOrderId,
                targetPaymentKey,
                -99800L,              // amount (취소 원금)
                -2250L,               // fee (환급 수수료)
                -205L,                // vat (환급 부가세)
                -97345L,              // payOutAmount (최종 정산 차감액)
                LocalDate.of(2023, 11, 30),
                mockCancelInfo        // 바인딩된 취소 객체 정보
        );

        List<TossSettlementResponse> requestBodyList = List.of(mockTossResponse);
        String jsonContent = objectMapper.writeValueAsString(requestBodyList);

        // ==========================================
        // 3. When: 컨트롤러 테스트 API 엔드포인트 HTTP POST 요청 수행
        // ==========================================
        mockMvc.perform(post("/api/settlement/test-reconcile")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()) // POST 요청을 위한 CSRF 토큰 보완
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk());

        // ==========================================
        // 4. Then: 영속성 레이어에서 음수 데이터 정합성 검증
        // ==========================================
        List<SettlementDetail> savedDetails = settlementDetailRepository.findAll();

        assertThat(savedDetails).hasSize(1);
        SettlementDetail actualDetail = savedDetails.get(0);

        // 정산 장부에 음수(-) 부호가 유실 없이 안전하게 기입되었는지 대조
        assertThat(actualDetail.getOrderId()).isEqualTo(targetOrderId);
        assertThat(actualDetail.getPaymentKey()).isEqualTo(targetPaymentKey);
        assertThat(actualDetail.getAmount()).isEqualTo(-99800L);
        assertThat(actualDetail.getFee()).isEqualTo(-2250L);
        assertThat(actualDetail.getVat()).isEqualTo(-205L);
        assertThat(actualDetail.getPayOutAmount()).isEqualTo(-97345L);

        // 상태 정합성 검증: 우리 DB(CANCELED) == 토스(마이너스 금액) 상황이 논리적으로 합치하므로 MATCHED 격상 확인
        assertThat(actualDetail.getStatus()).isEqualTo(SettlementStatus.MATCHED);
        assertThat(actualDetail.getPaymentHistory().getId()).isEqualTo(existingHistory.getId());
    }
    @Test
    @DisplayName("우리 DB 금액과 토스 정산 금액이 다르면 장부에 MISMATCHED_AMOUNT가 기록되고 구독 상태가 MANUAL_CHECK로 강제 전환된다.")
    void testReconcileFail_MismatchedAmount() throws Exception {
        // ==========================================
        // 1. Given: 우리 DB 상태 정의 (99,800원 정상 결제 성공 기록)
        // ==========================================
        String targetOrderId = "EjBNtZK7j8q2TlGFLJ-9T";
        String targetPaymentKey = "xLpgeoO7410238740297423RBKEzMjPJyG";
        Long ourDbAmount = 99800L; // 우리 DB 기록 금액

        PaymentHistory existingHistory = PaymentHistory.createSuccessHistory(
                testSubscription,
                targetPaymentKey,
                targetOrderId,
                ourDbAmount,
                "https://tosspayments.com/receipt/test"
        );
        paymentHistoryRepository.saveAndFlush(existingHistory);

        // ==========================================
        // 2. Given: 토스 정산 API에서 50,000원으로 불일치하는 금액을 던져준 상황 설정
        // ==========================================
        Long tossMismatchedAmount = 50000L; // 타 금액 유입

        TossSettlementResponse mockTossResponse = new TossSettlementResponse(
                targetOrderId,
                targetPaymentKey,
                tossMismatchedAmount, // 금액 불일치 발생시키기
                1125L,                // 변동된 수수료 가정
                102L,                 // 변동된 부가세 가정
                48773L,               // 변동된 실입금액 가정
                LocalDate.of(2023, 11, 30),
                null
        );

        List<TossSettlementResponse> requestBodyList = List.of(mockTossResponse);
        String jsonContent = objectMapper.writeValueAsString(requestBodyList);

        // ==========================================
        // 3. When: 컨트롤러 대조 API 엔드포인트 HTTP POST 요청 수행
        // ==========================================
        mockMvc.perform(post("/api/settlement/test-reconcile")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk());

        // ==========================================
        // 4. Then: 데이터베이스 최종 상태 대조 및 구독 격리 검증
        // ==========================================

        // [검증 포인트 1] SettlementDetail 장부 검증
        List<SettlementDetail> savedDetails = settlementDetailRepository.findAll();
        assertThat(savedDetails).hasSize(1);
        SettlementDetail actualDetail = savedDetails.get(0);

        assertThat(actualDetail.getOrderId()).isEqualTo(targetOrderId);
        // 장부의 status가 정해진 약속대로 MISMATCHED_AMOUNT 여야 함
        assertThat(actualDetail.getStatus()).isEqualTo(SettlementStatus.MISMATCHED_AMOUNT);

        // [검증 포인트 2] 사유(description) 필드 로그 상세 기록 검증
        // 사유 필드명(e.g. getDescription, getReason 등)은 실제 엔티티 구현체 필드명에 맞추어 수정하세요.
        assertThat(actualDetail.getDescription())
                .contains("금액 불일치")
                .contains(String.valueOf(ourDbAmount))
                .contains(String.valueOf(tossMismatchedAmount));

        // [검증 포인트 3] 회원 구독(Subscription) 상태가 MANUAL_CHECK로 변경되었는지 영속성 검증
        // 트랜잭션 내 엔티티 변화를 정확히 확인하기 위해 기존 구독 객체를 다시 조회하거나 영속성 컨텍스트를 새로고침합니다.
        Subscription updatedSubscription = subscriptionRepository.findById(testSubscription.getId())
                .orElseThrow();

        // 시스템 보호를 위해 비즈니스 메서드인 changeToManualCheck()가 실제로 호출되어 작동했는지 확인
        assertThat(updatedSubscription.getStatus())
                .isEqualTo(come.back.gotoday.payment.subscription.enums.SubscriptionStatus.MANUAL_CHECK);
    }

    @Test
    @DisplayName("우리 DB 상태(SUCCESS)와 토스 정산 데이터(취소/음수)의 상태가 불일치하면 장부에 MISMATCHED_STATUS가 기록되고 구독 상태가 MANUAL_CHECK로 강제 전환된다.")
    void testReconcileFail_MismatchedStatus() throws Exception {
        // ==========================================
        // 1. Given: 실제 우리 DB 상태 정의 (99,800원 정상 결제 성공 기록)
        // ==========================================
        String targetOrderId = "EjBNtZK7j8q2TlGFLJ-9T";
        String targetPaymentKey = "xLpgeoO7410238740297423RBKEzMjPJyG";
        Long orderAmount = 99800L;

        // 우리 DB는 분명히 SUCCESS(결제 성공) 상태로 기록해 둠
        PaymentHistory existingHistory = PaymentHistory.createSuccessHistory(
                testSubscription,
                targetPaymentKey,
                targetOrderId,
                orderAmount,
                "https://tosspayments.com/receipt/test"
        );
        paymentHistoryRepository.saveAndFlush(existingHistory);

        // ==========================================
        // 2. Given: 토스 정산 API에서는 '취소 건(마이너스 금액 및 CancelInfo 포함)'으로 데이터가 넘어온 상황 설정
        // ==========================================
        come.back.gotoday.external.toss.dto.SettlementDto.CancelInfo mockCancelInfo =
                new come.back.gotoday.external.toss.dto.SettlementDto.CancelInfo(
                        "토스 어드민에서 강제 취소됨",
                        orderAmount,
                        "DONE"
                );

        // 우리 DB는 SUCCESS인데 토스는 마이너스 금액(-99,800원)을 던져 결제 상태 불일치 유도
        TossSettlementResponse mockTossResponse = new TossSettlementResponse(
                targetOrderId,
                targetPaymentKey,
                -orderAmount,         // 취소 금액 유입
                -2250L,
                -205L,
                -97345L,
                LocalDate.of(2023, 11, 30),
                mockCancelInfo        // 취소 정보 객체 포함
        );

        List<TossSettlementResponse> requestBodyList = List.of(mockTossResponse);
        String jsonContent = objectMapper.writeValueAsString(requestBodyList);

        // ==========================================
        // 3. When: 컨트롤러 대조 API 엔드포인트 HTTP POST 요청 수행
        // ==========================================
        mockMvc.perform(post("/api/settlement/test-reconcile")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk());

        // ==========================================
        // 4. Then: 데이터베이스 최종 상태 대조 및 구독 격리 검증
        // ==========================================

        // [검증 포인트 1] SettlementDetail 장부 검증
        List<SettlementDetail> savedDetails = settlementDetailRepository.findAll();
        assertThat(savedDetails).hasSize(1);
        SettlementDetail actualDetail = savedDetails.get(0);

        assertThat(actualDetail.getOrderId()).isEqualTo(targetOrderId);
        // 장부의 status가 약속된 상태 불일치 오류 코드(MISMATCHED_STATUS)여야 함
        assertThat(actualDetail.getStatus()).isEqualTo(SettlementStatus.MISMATCHED_STATUS);

        // [검증 포인트 2] 회원 구독(Subscription) 상태가 MANUAL_CHECK로 긴급 안전 전환되었는지 영속성 검증
        Subscription updatedSubscription = subscriptionRepository.findById(testSubscription.getId())
                .orElseThrow();

        // 비즈니스 메서드 changeToManualCheck() 작동 여부 확인
        assertThat(updatedSubscription.getStatus())
                .isEqualTo(come.back.gotoday.payment.subscription.enums.SubscriptionStatus.MANUAL_CHECK);
    }

    @Test
    @DisplayName("우리 DB에 없는 orderId가 토스 정산 데이터로 유입되면, NPE 없이 장부에 NOT_FOUND_PAYMENT 상태로 저장된다.")
    void testReconcileFail_NotFoundPayment() throws Exception {
        // ==========================================
        // 1. Given: 우리 DB 상태 정의 (아무런 결제 기록도 생성하지 않음)
        // ==========================================
        // 존재하지 않는 가상의 주문 ID 정의
        String missingOrderId = "MISSING-ORDER-ID-9999X";
        String targetPaymentKey = "xLpgeoO7410238740297423RBKEzMjPJyG";
        Long unknownOrderAmount = 49900L;

        // 의도적으로 paymentHistoryRepository.save()를 수행하지 않고 비워둠

        // ==========================================
        // 2. Given: 토스 정산 API에서는 해당 주문 ID로 정산 데이터가 날아온 상황 설정
        // ==========================================
        TossSettlementResponse mockTossResponse = new TossSettlementResponse(
                missingOrderId,       // 우리 DB에는 없는 ID
                targetPaymentKey,
                unknownOrderAmount,
                1100L,                // fee
                100L,                 // vat
                48700L,               // payOutAmount
                LocalDate.of(2023, 11, 30),
                null
        );

        List<TossSettlementResponse> requestBodyList = List.of(mockTossResponse);
        String jsonContent = objectMapper.writeValueAsString(requestBodyList);

        // ==========================================
        // 3. When: 컨트롤러 대조 API 엔드포인트 HTTP POST 요청 수행
        // ==========================================
        // 시스템(배치/루프)이 뻗지 않고 정상 응답(200 OK)을 반환하는지 확인
        mockMvc.perform(post("/api/settlement/test-reconcile")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk());

        // ==========================================
        // 4. Then: 데이터베이스 결과 장부 및 연관관계 Null 검증
        // ==========================================
        List<SettlementDetail> savedDetails = settlementDetailRepository.findAll();
        assertThat(savedDetails).hasSize(1);
        SettlementDetail actualDetail = savedDetails.get(0);

        // 유입된 외부 데이터 정보 자체는 장부에 기록되어야 추적이 가능함
        assertThat(actualDetail.getOrderId()).isEqualTo(missingOrderId);
        assertThat(actualDetail.getAmount()).isEqualTo(unknownOrderAmount);

        // [검증 포인트 1] 장부의 status가 약속된 매칭 실패 코드(NOT_FOUND_PAYMENT)여야 함
        assertThat(actualDetail.getStatus()).isEqualTo(SettlementStatus.NOT_FOUND_PAYMENT);

        // [검증 포인트 2] 매핑될 결제 이력이 없으므로 외래키(PaymentHistory) 자리가 null로 채워져야 함
        // (SettlementDetail 엔티티 내 @ManyToOne(optional = true) 혹은 @JoinColumn(nullable = true) 처리 필요)
        assertThat(actualDetail.getPaymentHistory()).isNull();
    }
}