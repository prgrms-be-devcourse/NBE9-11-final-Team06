package come.back.gotoday.payment.subscription.service;

import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.external.toss.dto.TossAutomatedPaymentRequest;
import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.payment.billing.config.BillingKeyConverter;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "KMA_WEATHER_API_KEY=mock", "weather.kma.service-key=mock",
        "SEOUL_CROWD_AREA_NAMES=강남", "TOUR_API_KEY=mock"
})
@ActiveProfiles("test")
@Transactional // 개별 테스트 완료 후 실제 데이터 전원 롤백 보장
@DisplayName("SubscriptionBatchFacade 통합 테스트 (실제 DB + 외부 API 격리)")
class SubscriptionBatchFacadeIntegrationTest {

    @Autowired
    private SubscriptionBatchFacade subscriptionBatchFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private TossPaymentsClient tossPaymentsClient;

    @Autowired
    private BillingKeyConverter billingKeyConverter;

    private Long savedMemberId;
    private Long savedBillingInfoId;
    private Long savedPlanId;
    private Long savedSubscriptionId;

    @BeforeEach
    void setUp() {
        // 1. 기초 데이터셋 세팅
        jdbcTemplate.update("INSERT INTO `member` (email, password, nickname, role, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "batch@test.com", "hash", "배치유저", "ROLE_USER", "ACTIVE", LocalDateTime.now(), LocalDateTime.now());
        savedMemberId = jdbcTemplate.queryForObject("SELECT id FROM `member` WHERE email = ?", Long.class, "batch@test.com");

        String encryptedBillingKey = billingKeyConverter.convertToDatabaseColumn("toss-billing-key-123");
        jdbcTemplate.update("INSERT INTO `billing_info` (customer_key, billing_key, card_company, card_number, status, member_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "cust-batch-key",
                encryptedBillingKey,
                "국민", "5555", "ACTIVE", savedMemberId, LocalDateTime.now(), LocalDateTime.now());savedBillingInfoId = jdbcTemplate.queryForObject("SELECT id FROM `billing_info` WHERE customer_key = ?", Long.class, "cust-batch-key");

        jdbcTemplate.update("INSERT INTO `plan` (name, display_name, amount, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                "BATCH_PREMIUM", "프리미엄 정기 요금제", 9900L, true, LocalDateTime.now(), LocalDateTime.now());
        savedPlanId = jdbcTemplate.queryForObject("SELECT id FROM `plan` WHERE name = ?", Long.class, "BATCH_PREMIUM");
    }

    private void createSubscription(SubscriptionStatus status, LocalDate nextBillingDate) {
        String sql = "INSERT INTO `subscription` (amount, next_billing_date, payment_day, status, payment_failed_at, billing_info_id, plan_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, 9900L);
            ps.setString(2, nextBillingDate.toString());
            ps.setInt(3, nextBillingDate.getDayOfMonth());
            ps.setString(4, status.name());

            // 유예 상태일 때만 8일 전 실패일을 기록, ACTIVE일 때는 null
            if (status == SubscriptionStatus.EXPIRED_PAYMENT_PENDING) {
                ps.setString(5, nextBillingDate.toString());
            } else {
                ps.setNull(5, java.sql.Types.DATE);
            }

            ps.setLong(6, savedBillingInfoId);
            ps.setLong(7, savedPlanId);
            ps.setString(8, LocalDateTime.now().toString());
            ps.setString(9, LocalDateTime.now().toString());
            return ps;
        }, keyHolder);

        savedSubscriptionId = keyHolder.getKey().longValue();

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("executeScheduledPayment 메서드 실행 시")
    class Describe_executeScheduledPayment {

        @Test
        @DisplayName("[성공] 토스 결제가 승인되면, 구독 상태가 ACTIVE가 되고 결제 성공 장부가 영속화된다.")
        void it_completes_scheduled_payment_successfully() {
            // given
            createSubscription(SubscriptionStatus.ACTIVE, LocalDate.now().minusDays(1));


            TossAutomatedPaymentResponse.ReceiptInfo mockReceipt = new TossAutomatedPaymentResponse.ReceiptInfo(
                    "https://receipt.toss.im/example"
            );

            TossAutomatedPaymentResponse realTossResponse = new TossAutomatedPaymentResponse(
                    "mock-mid",
                    "mock-tx-key",
                    "toss-success-payment-key-2026",
                    "ORD-BATCH-MOCK",
                    "프리미엄 정기 요금제",
                    "DONE",
                    "2026-06-29T11:00:00",
                    "2026-06-29T11:01:00",
                    "BILLING",
                    "카드",
                    9900L,
                    null,
                    mockReceipt
            );

            when(tossPaymentsClient.requestPayment(any(), any()))
                    .thenReturn(realTossResponse);

            // when
            subscriptionBatchFacade.executeScheduledPayment(savedSubscriptionId);
            em.flush();
            em.clear();

            // then
            String currentStatus = jdbcTemplate.queryForObject("SELECT status FROM `subscription` WHERE id = ?", String.class, savedSubscriptionId);
            LocalDate currentNextDate = jdbcTemplate.queryForObject("SELECT next_billing_date FROM `subscription` WHERE id = ?", LocalDate.class, savedSubscriptionId);

            assertThat(currentStatus).isEqualTo("ACTIVE");
            assertThat(currentNextDate).isEqualTo(LocalDate.now().minusDays(1).plusMonths(1));

            Integer historyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `payment_history` WHERE subscription_id = ? AND status = 'SUCCESS'", Integer.class, savedSubscriptionId);
            assertThat(historyCount).isEqualTo(1);
        }
        @Test
        @DisplayName("[유예 기간 초과] 이미 유예중인 유저가 7일(GRACE_PERIOD_DAYS)을 초과한 경우 강제 해지 처리된다.")
        void it_terminates_subscription_when_grace_period_is_over() {
            // given
            LocalDate eightDaysAgo = LocalDate.now().minusDays(8);
            createSubscription(SubscriptionStatus.EXPIRED_PAYMENT_PENDING, eightDaysAgo);


            jdbcTemplate.update("UPDATE `subscription` SET payment_failed_at = ? WHERE id = ?", eightDaysAgo, savedSubscriptionId);
            em.flush();
            em.clear();

            // when
            subscriptionBatchFacade.executeScheduledPayment(savedSubscriptionId);
            em.flush();
            em.clear();

            // then
            verify(tossPaymentsClient, never()).requestPayment(anyString(), any());

            String currentStatus = jdbcTemplate.queryForObject("SELECT status FROM `subscription` WHERE id = ?", String.class, savedSubscriptionId);
            assertThat(currentStatus).isEqualTo("CANCELED");

            String failureReason = jdbcTemplate.queryForObject("SELECT failure_reason FROM `payment_history` WHERE subscription_id = ?", String.class, savedSubscriptionId);
            assertThat(failureReason).contains("유예 기간 초과");
        }

        @Test
        @DisplayName("[경우 A: 네트워크 타임아웃] 토스 통신 지연 시 실패 처리를 미루고 PENDING 격리 상태를 유지한다.")
        void it_keeps_pending_status_when_network_timeout_occurs() {
            // given
            createSubscription(SubscriptionStatus.ACTIVE, LocalDate.now().minusDays(1));

            when(tossPaymentsClient.requestPayment(anyString(), any(TossAutomatedPaymentRequest.class)))
                    .thenThrow(new ResourceAccessException("I/O 타임아웃 발생"));

            // when
            subscriptionBatchFacade.executeScheduledPayment(savedSubscriptionId);
            em.flush();
            em.clear();

            // then
            // 1. 비즈니스 원자성에 의해 API 유입 전 강제 저장한 'PENDING' 상태가 그대로 유지되는가 검증
            String currentStatus = jdbcTemplate.queryForObject("SELECT status FROM `subscription` WHERE id = ?", String.class, savedSubscriptionId);
            assertThat(currentStatus).isEqualTo("PENDING");

            // 2. 섣부른 실패 내역 장부가 적재되지 않았는지 차단 확인
            Integer historyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `payment_history` WHERE subscription_id = ?", Integer.class, savedSubscriptionId);
            assertThat(historyCount).isZero();
        }

        @Test
        @DisplayName("[경우 B: 결제 거절] 잔액 부족 등 토스 에러 응답 수신 시, 유예 상태 전환 및 실패 장부를 생성한다.")
        void it_transitions_to_grace_status_when_toss_rejects_payment() {
            // given
            createSubscription(SubscriptionStatus.ACTIVE, LocalDate.now().minusDays(1));

            // RestClientResponseException 시뮬레이션 데이터 조립
            RestClientResponseException mockException = new RestClientResponseException(
                    "잔액부족 거절", 400, "Bad Request", null,
                    "{\"message\":\"잔액이 부족합니다.\"}".getBytes(StandardCharsets.UTF_8), null
            );

            when(tossPaymentsClient.requestPayment(anyString(), any(TossAutomatedPaymentRequest.class)))
                    .thenThrow(mockException);

            // when
            subscriptionBatchFacade.executeScheduledPayment(savedSubscriptionId);
            em.flush();
            em.clear();

            // then
            // 1. 구독 상태가 유예실패(EXPIRED_PAYMENT_PENDING) 상태로 갱신 완료되었는지 확인
            String currentStatus = jdbcTemplate.queryForObject("SELECT status FROM `subscription` WHERE id = ?", String.class, savedSubscriptionId);
            assertThat(currentStatus).isEqualTo("EXPIRED_PAYMENT_PENDING");

            // 2. 실패 이력이 실패 사유 바디값과 함께 DB 테이블에 안착했는지 확인
            String dbFailureReason = jdbcTemplate.queryForObject("SELECT failure_reason FROM `payment_history` WHERE subscription_id = ?", String.class, savedSubscriptionId);
            assertThat(dbFailureReason).contains("잔액이 부족합니다");
        }

        @Test
        @DisplayName("[동시성 방어] 이미 다른 프로세스에 의해 결제 대기(PENDING) 중인 구독은 결제를 중복 진행하지 않는다.")
        void it_skips_processing_when_already_pending() {
            // given
            createSubscription(SubscriptionStatus.PENDING, LocalDate.now().minusDays(1)); // 이미 PENDING 선점된 데이터

            // when
            subscriptionBatchFacade.executeScheduledPayment(savedSubscriptionId);

            // then
            // 결제 요청 레이어가 가동조차 되지 않아야 동시성 더블 결제를 막을 수 있습니다.
            verify(tossPaymentsClient, never()).requestPayment(anyString(), any());
        }
    }
}