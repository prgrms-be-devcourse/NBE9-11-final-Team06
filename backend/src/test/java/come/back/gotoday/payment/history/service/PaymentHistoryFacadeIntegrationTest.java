package come.back.gotoday.payment.history.service;


import come.back.gotoday.external.toss.dto.TossCancelRequest;
import come.back.gotoday.external.toss.dto.TossCancelResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.history.enums.PaymentStatus;
import come.back.gotoday.payment.subscription.dto.SubscriptionPaymentCancelRequest;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("PaymentHistoryFacade 통합 테스트 (Spring Boot + H2)")
class PaymentHistoryFacadeIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private PaymentHistoryFacade paymentHistoryFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate; // JPA 엔티티 구조에 종속되지 않고 주신 DDL 그대로 데이터 조작을 위해 사용

    @Autowired
    private jakarta.persistence.EntityManager em;

    // 테스트 픽스처 ID 정의
    private final Long MEMBER_ID = 999L;
    private final Long BILLING_INFO_ID = 888L;
    private final Long PLAN_ID = 777L;
    private final Long SUBSCRIPTION_ID = 666L;
    private final Long PAYMENT_HISTORY_ID = 555L;

    private final String PAYMENT_KEY = "toss_payment_key_valid_2026";
    private final String CANCEL_REASON = "사용자 변심으로 인한 정기 결제 당일 전면 취소 요청";

    @BeforeEach
    void setUp() {
        // [DDL 완벽 준수] 질문 1번의 FK 체인을 형성하기 위해 역순으로 기초 데이터 정밀 Insert

        // 1. member 테이블 생성
        jdbcTemplate.update("INSERT INTO `member` (id, email, password, nickname, role, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                MEMBER_ID, "test@gotoday.com", "hashed_password", "고투데이유저", "USER", "ACTIVE", LocalDateTime.now(), LocalDateTime.now());

        // 2. billing_info 테이블 생성
        jdbcTemplate.update("INSERT INTO `billing_info` (id, customer_key, billing_key, card_company, card_number, status, member_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                BILLING_INFO_ID,
                "cust_123",
                "bill_123",
                "신한카드",
                "1234-****-****-****",
                "ACTIVE", // BillingStatus.ACTIVE.name()
                MEMBER_ID,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        // 3. plan 테이블 생성
        jdbcTemplate.update("INSERT INTO `plan` (id, name, display_name, amount, is_active, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                PLAN_ID, "PREMIUM_1M", "프리미엄 1개월 정기권", 9900L, true, LocalDateTime.now(), LocalDateTime.now());

        // 4. subscription 테이블 생성 (최초 상태: ACTIVE)
        jdbcTemplate.update("INSERT INTO `subscription` (id, amount, next_billing_date, payment_day, status, billing_info_id, plan_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                SUBSCRIPTION_ID, 9900L, LocalDate.now().plusMonths(1), 29L, SubscriptionStatus.ACTIVE.name(), BILLING_INFO_ID, PLAN_ID, LocalDateTime.now(), LocalDateTime.now());

        // 5. payment_history 테이블 생성 (최초 상태: SUCCESS)
        jdbcTemplate.update("INSERT INTO `payment_history` (id, order_id, payment_key, amount, status, subscription_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                PAYMENT_HISTORY_ID, "order_20260629_01", PAYMENT_KEY, 9900L, PaymentStatus.SUCCESS.name(), SUBSCRIPTION_ID, LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("결제 취소 유스케이스 실행 시")
    class Describe_CancelPaymentUseCase {

        @Test
        @DisplayName("토스 API가 정상 응답을 주면, 진짜 DB의 결제 이력은 CANCELED, 구독 상태도 CANCELED로 영구 해지 업데이트된다.")
        void it_updates_real_database_status_to_canceled() {
            // given
            SubscriptionPaymentCancelRequest request = new SubscriptionPaymentCancelRequest(CANCEL_REASON);

            // 토스 외부 API 성공 스펙 세팅
            TossCancelResponse mockResponse = new TossCancelResponse(
                    PAYMENT_KEY, "order_20260629_01", "프리미엄 1개월 정기권", "CANCELED", 9900L,
                    "2026-06-29T11:00:00+09:00", "2026-06-29T11:01:00+09:00",
                    Collections.emptyList(), new TossCancelResponse.ReceiptInfo("https://receipt.url")
            );
            when(tossPaymentsClient.cancelPayment(eq(PAYMENT_KEY), any(TossCancelRequest.class)))
                    .thenReturn(mockResponse);

            // when
            paymentHistoryFacade.cancelPayment(MEMBER_ID, PAYMENT_HISTORY_ID, request);
            em.flush();
            em.clear();
            // then
            // 1. 외부 API가 의도된 파라미터로 올바르게 호출되었는지 Mock 검증
            verify(tossPaymentsClient, times(1)).cancelPayment(eq(PAYMENT_KEY), any(TossCancelRequest.class));

            // 2. [진짜 DB 검증] payment_history 테이블의 status 상태 변경값 검증
            Map<String, Object> updatedHistory = jdbcTemplate.queryForMap(
                    "SELECT status FROM `payment_history` WHERE id = ?", PAYMENT_HISTORY_ID);
            assertThat(updatedHistory.get("status")).isEqualTo(PaymentStatus.CANCELED.name());

            // 3. [진짜 DB 검증] 질문 2번 기반 subscription 테이블의 status 상태 변경값 검증
            Map<String, Object> updatedSubscription = jdbcTemplate.queryForMap(
                    "SELECT status FROM `subscription` WHERE id = ?", SUBSCRIPTION_ID);
            assertThat(updatedSubscription.get("status")).isEqualTo(SubscriptionStatus.CANCELED.name());
        }

        @Test
        @DisplayName("타인의 결제 이력 ID로 해지를 시도하는 경우 권한 예외가 발생하고 DB 상태는 그대로 유지된다.")
        void it_fails_with_unauthorized_member_and_does_not_modify_db() {
            // given
            Long unauthorizedMemberId = 111L; // 데이터와 매핑되지 않는 다른 유저 ID
            SubscriptionPaymentCancelRequest request = new SubscriptionPaymentCancelRequest(CANCEL_REASON);

            // when & then
            // 내부 서비스 검증(getPaymentKeyValidated) 로직에 의해 예외 처리가 발생해야 함
            assertThatThrownBy(() -> paymentHistoryFacade.cancelPayment(unauthorizedMemberId, PAYMENT_HISTORY_ID, request))
                    .isInstanceOf(BusinessException.class);

            // 외부 API 요청이 차단되었는지 확인
            verify(tossPaymentsClient, never()).cancelPayment(any(), any());

            // DB 값이 수정되지 않고 최초 상태인 SUCCESS와 ACTIVE를 고수하는지 검증
            String currentHistoryStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM `payment_history` WHERE id = ?", String.class, PAYMENT_HISTORY_ID);
            String currentSubscriptionStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM `subscription` WHERE id = ?", String.class, SUBSCRIPTION_ID);

            assertThat(currentHistoryStatus).isEqualTo(PaymentStatus.SUCCESS.name());
            assertThat(currentSubscriptionStatus).isEqualTo(SubscriptionStatus.ACTIVE.name());
        }

        @Test
        @DisplayName("토스 API가 정상 완료 코드가 아닌 실패 상태를 응답하면, EXTERNAL_API_ERROR를 던지고 DB 롤백이 일어난다.")
        void it_rolls_back_and_keeps_db_intact_when_toss_api_fails() {
            // given
            SubscriptionPaymentCancelRequest request = new SubscriptionPaymentCancelRequest(CANCEL_REASON);

            // status가 CANCELED가 아닌 잘못된 응답 반환 시뮬레이션
            TossCancelResponse mockFailResponse = new TossCancelResponse(
                    PAYMENT_KEY, "order_20260629_01", "프리미엄 1개월 정기권", "REJECTED", 9900L,
                    "2026-06-29T11:00:00+09:00", "2026-06-29T11:01:00+09:00",
                    Collections.emptyList(), new TossCancelResponse.ReceiptInfo("https://receipt.url")
            );
            when(tossPaymentsClient.cancelPayment(eq(PAYMENT_KEY), any(TossCancelRequest.class)))
                    .thenReturn(mockFailResponse);

            // when & then
            assertThatThrownBy(() -> paymentHistoryFacade.cancelPayment(MEMBER_ID, PAYMENT_HISTORY_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);

            // 최종 내부 DB 변경 로직이 차단되어 최초 상태(SUCCESS / ACTIVE)를 안전하게 유지하는지 확인
            String currentHistoryStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM `payment_history` WHERE id = ?", String.class, PAYMENT_HISTORY_ID);
            String currentSubscriptionStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM `subscription` WHERE id = ?", String.class, SUBSCRIPTION_ID);

            assertThat(currentHistoryStatus).isEqualTo(PaymentStatus.SUCCESS.name());
            assertThat(currentSubscriptionStatus).isEqualTo(SubscriptionStatus.ACTIVE.name());
        }
    }

}