package come.back.gotoday.payment.subscription.entity;

import come.back.gotoday.payment.billing.entity.BillingInfo;
import come.back.gotoday.payment.plan.entity.Plan;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외래키 설정: 어떤 카드(빌링키)로 결제할 것인가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_info_id", nullable = false)
    private BillingInfo billingInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false)
    private Long amount; // 매달 결제될 정기 결제 금액

    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate; // 다음 자동 결제 예정일 (배치 스케줄러 기준 조회 대상)


    // [추가] 최초 구독 시 결정된 고정 기준 결제일 (예: 31)
    @Column(name = "payment_day", nullable = false)
    private int paymentDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status; // 구독 상태 (ACTIVE, PAUSED, CANCELED)

    // 직접 추가된 생성 시간 및 업데이트 시간 필드
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "payment_failed_at")
    private LocalDate paymentFailedAt; // 자동 결제 실패일 (정상 결제 시 null로 초기화)

    private Subscription(BillingInfo billingInfo,Plan plan,Long amount, LocalDate nextBillingDate, int paymentDay, SubscriptionStatus status) {
        validateAmount(amount); // [추가] 금액 검증

        this.billingInfo = billingInfo;
        this.plan = plan;
        this.amount = amount;
        this.nextBillingDate = nextBillingDate;
        this.paymentDay = paymentDay;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Subscription startSubscription(BillingInfo billingInfo, Plan plan, Long amount, LocalDate startDate) {
        return new Subscription(
                billingInfo,
                plan,
                amount,
                startDate,
                startDate.getDayOfMonth(), // 시작일의 '일'을 기준 결제일로 저장
                SubscriptionStatus.PENDING
        );
    }

    /**
     * 비즈니스 요구사항에 따라 다음과 같이 유연하게 생성 메서드를 추가할 수 있습니다.
     */
    public static Subscription startFreePromotionSubscription(BillingInfo billingInfo, Plan plan, Long amount, LocalDate startDate) {
        return new Subscription(
                billingInfo,
                plan,
                amount,
                startDate.plusMonths(2), // 첫 달 무료이므로 다음 결제일은 2달 뒤!
                startDate.getDayOfMonth(),
                SubscriptionStatus.ACTIVE
        );
    }

    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
        // 첫 결제가 완료되었으므로 다음 자동 결제일을 한 달 뒤로 미룹니다.
        renewNextBillingDate();
        this.updatedAt = LocalDateTime.now();
    }

    //  결제 성공 시 다음 결제일 갱신
    public void renewNextBillingDate() {
        // 1. 현재 '다음 결제일'에서 단순히 한 달을 더해 기준 연/월을 구합니다.
        LocalDate nextMonthDate = this.nextBillingDate.plusMonths(1);

        // 2. 해당 연/월의 최대 일수(마지막 날)를 구합니다. (예: 2월이면 28일 또는 29일)
        int maxDayOfMonth = nextMonthDate.lengthOfMonth();

        // 3. 원래 기준일(paymentDay)이 그 달의 최대 일수보다 크다면, 그 달의 마지막 날로 지정합니다.
        //    그렇지 않다면 원래 기준일(paymentDay)을 그대로 사용합니다.
        int targetDay = Math.min(this.paymentDay, maxDayOfMonth);

        // 4. 최종 보정된 날짜로 세팅합니다. (withDayOfMonth 사용)
        this.nextBillingDate = nextMonthDate.withDayOfMonth(targetDay);
        this.paymentFailedAt = null; // 실패 기록 초기화
        this.updatedAt = LocalDateTime.now();
    }

    // 구독 해지 요청 처리
    public void reserveCancellation() {
        this.status = SubscriptionStatus.CANCELED_RESERVED;
        this.updatedAt = LocalDateTime.now();
    }
    //강제 해지 (결제 실패, 유예 기간 만료, 결제 취소 등 즉시 종료)
    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
        this.updatedAt = LocalDateTime.now(); // 데이터 변경 시 업데이트 시간 갱신
    }

    public void changeToManualCheck() {
        this.status = SubscriptionStatus.MANUAL_CHECK;
    }
    public void changeStatus() {
        this.status = SubscriptionStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }
    /**
     *  결제 실패 시 즉시 해지하지 않고 유예 상태로 전환합니다.
     * 이때 최초 실패라면 실패 날짜를 기록합니다.
     */
    public void changeToPaymentBatchFail(LocalDate today) {
        this.status = SubscriptionStatus.EXPIRED_PAYMENT_PENDING;
        if (this.paymentFailedAt == null) {
            this.paymentFailedAt = today; // 최초 실패일 기록
        }
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isGracePeriodExpired(LocalDate today, int graceDays) {
        if (this.paymentFailedAt == null) return false;
        return this.paymentFailedAt.plusDays(graceDays).isBefore(today);
    }

    // 결제 금액 검증 비즈니스 로직
    private void validateAmount(Long amount) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("정기 결제 금액은 0원 이상이어야 합니다.");
        }
    }
}