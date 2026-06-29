package come.back.gotoday.payment.settlement.service;

import come.back.gotoday.external.toss.dto.SettlementDto;
import come.back.gotoday.payment.history.enums.PaymentStatus;
import come.back.gotoday.payment.settlement.entity.SettlementDetail;
import come.back.gotoday.payment.settlement.enums.SettlementStatus;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "KMA_WEATHER_API_KEY=mock_api_key",
        "weather.kma.service-key=mock_api_key",
        "SEOUL_CROWD_AREA_NAMES=강남역,홍대입구역",
        "TOUR_API_KEY=mock_tour_api_key"
})
@ActiveProfiles("test")
@Transactional
@DisplayName("SettlementService 통합 테스트 (실제 DB 연동)")
class SettlementServiceIntegrationTest {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager em;

    private final String ORDER_ID = "integration-order-1234";
    private final String PAYMENT_KEY = "toss-key-1234";
    private final LocalDate PAID_OUT_DATE = LocalDate.of(2026, 6, 29); // 💡 LocalDate 스펙 적용

    private Long savedSubscriptionId;

    @BeforeEach
    void setUp() {
        // 1. 기초 유저 생성 (중복 방어)
        Integer memberCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `member` WHERE email = ?", Integer.class, "settle@test.com");
        if (memberCount == 0) {
            jdbcTemplate.update("INSERT INTO `member` (email, password, nickname, role, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    "settle@test.com", "pass", "정산맨", "ROLE_USER", "ACTIVE", LocalDateTime.now(), LocalDateTime.now());
        }
        Long memberId = jdbcTemplate.queryForObject("SELECT id FROM `member` WHERE email = ?", Long.class, "settle@test.com");

        // 2. 빌링 정보 생성 (중복 방어)
        Integer billingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `billing_info` WHERE customer_key = ?", Integer.class, "cust-1");
        if (billingCount == 0) {
            jdbcTemplate.update("INSERT INTO `billing_info` (customer_key, billing_key, card_company, card_number, status, member_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    "cust-1", "bill-1", "신한", "1234", "ACTIVE", memberId, LocalDateTime.now(), LocalDateTime.now());
        }
        Long billingInfoId = jdbcTemplate.queryForObject("SELECT id FROM `billing_info` WHERE customer_key = ?", Long.class, "cust-1");

        // 3. [문제의 구간] 요금제(Plan) 생성 전 중복 체크 후 분기 처리
        Integer planCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `plan` WHERE name = ?", Integer.class, "PREMIUM_PLAN");
        if (planCount == 0) {
            // DB에 없을 때만 안전하게 인서트 진행
            jdbcTemplate.update(
                    "INSERT INTO `plan` (name, display_name, amount, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                    "PREMIUM_PLAN", "프리미엄 구독", 9900L, true, LocalDateTime.now(), LocalDateTime.now()
            );
        }
        Long planId = jdbcTemplate.queryForObject("SELECT id FROM `plan` WHERE name = ?", Long.class, "PREMIUM_PLAN");

        // 4. 구독(Subscription) 생성 (중복 방어)
        Integer subscriptionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `subscription` WHERE billing_info_id = ?", Integer.class, billingInfoId);
        if (subscriptionCount == 0) {
            jdbcTemplate.update("INSERT INTO `subscription` (amount, next_billing_date, payment_day, status, billing_info_id, plan_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    9900L, LocalDate.now().plusMonths(1), 15, "ACTIVE", billingInfoId, planId, LocalDateTime.now(), LocalDateTime.now());
        }
        savedSubscriptionId = jdbcTemplate.queryForObject("SELECT id FROM `subscription` WHERE billing_info_id = ?", Long.class, billingInfoId);

        // 5. 우리 장부 결제 이력(PaymentHistory) 생성 (중복 방어)
        Integer historyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `payment_history` WHERE order_id = ?", Integer.class, ORDER_ID);
        if (historyCount == 0) {
            jdbcTemplate.update("INSERT INTO `payment_history` (order_id, payment_key, amount, status, subscription_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    ORDER_ID, PAYMENT_KEY, 9900L, "SUCCESS", savedSubscriptionId, LocalDateTime.now(), LocalDateTime.now());
        }

        // 영속성 컨텍스트 초기화 (DB와 싱크 정렬)
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("토스 정산 데이터와 우리 DB 장부의 상태가 불일치할 경우(취소건 유입), 자동으로 불일치를 감지하고 구독 상태를 MANUAL_CHECK로 바꾼다.")
    void it_detects_mismatch_status_and_changes_subscription_to_manual_check() {
        // given
        // 💡 실제 CancelInfo 레코드 객체를 생성하여 DTO에 매핑
        SettlementDto.CancelInfo cancelInfo = new SettlementDto.CancelInfo("고객 요청 해지", 9900L, "DONE");

        SettlementDto.TossSettlementResponse tossCancelResponse = new SettlementDto.TossSettlementResponse(
                ORDER_ID, PAYMENT_KEY, -9900L, -300L, -30L, -10230L, PAID_OUT_DATE, cancelInfo
        );

        // when
        settlementService.reconcileSettlement(List.of(tossCancelResponse));

        // 쓰기 지연 반영 및 캐시 삭제
        em.flush();
        em.clear();

        // then
        // 1. 정산 상세 장부에 MISMATCHED_STATUS로 이력이 남았는지 확인
        String detailStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM `settlement_detail` WHERE order_id = ?", String.class, ORDER_ID);
        assertThat(detailStatus).isEqualTo("MISMATCHED_STATUS");

        // 2. 구독 정보가 'MANUAL_CHECK' 상태로 격리 변환되었는지 확인
        String subscriptionStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM `subscription` WHERE id = ?", String.class, savedSubscriptionId);
        assertThat(subscriptionStatus).isEqualTo(SubscriptionStatus.MANUAL_CHECK.name());
    }

    @Test
    @DisplayName("토스 데이터와 우리 장부의 상태 및 금액이 완벽히 매칭되면 MATCHED 상태로 정산 내역 확정 장부를 저장한다.")
    void it_saves_perfectly_matched_settlement_detail() {
        // given
        // 💡 정상 승인이므로 cancel 필드에 null 주입
        SettlementDto.TossSettlementResponse tossSuccessResponse = new SettlementDto.TossSettlementResponse(
                ORDER_ID, PAYMENT_KEY, 9900L, 300L, 30L, 9570L, PAID_OUT_DATE, null
        );

        // when
        settlementService.reconcileSettlement(List.of(tossSuccessResponse));
        em.flush();
        em.clear();

        // then
        // 1. 정상 매칭(MATCHED) 확인
        String detailStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM `settlement_detail` WHERE order_id = ?", String.class, ORDER_ID);
        assertThat(detailStatus).isEqualTo("MATCHED");

        // 2. 구독 상태가 변하지 않고 원래의 ACTIVE를 유지하는지 확인
        String subscriptionStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM `subscription` WHERE id = ?", String.class, savedSubscriptionId);
        assertThat(subscriptionStatus).isEqualTo("ACTIVE");
    }
}