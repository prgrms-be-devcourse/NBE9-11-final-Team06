package come.back.gotoday.payment.subscription.entity;

import come.back.gotoday.payment.billing.entity.BillingInfo;
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
public class Subscription { // BaseEntity 상속 제거

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외래키 설정: 어떤 카드(빌링키)로 결제할 것인가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_info_id", nullable = false)
    private BillingInfo billingInfo;

    @Column(nullable = false)
    private String planName; // 이용 중인 요금제/상품명 (예: 프리미엄 멤버십)

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


    private Subscription(BillingInfo billingInfo, String planName, Long amount, LocalDate nextBillingDate, int paymentDay, SubscriptionStatus status) {
        validateAmount(amount); // [추가] 금액 검증

        this.billingInfo = billingInfo;
        this.planName = planName;
        this.amount = amount;
        this.nextBillingDate = nextBillingDate;
        this.paymentDay = paymentDay;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Subscription startSubscription(BillingInfo billingInfo, String planName, Long amount, LocalDate startDate) {
        LocalDate nextBillingDate = startDate.plusMonths(1);

        return new Subscription(
                billingInfo,
                planName,
                amount,
                nextBillingDate,
                startDate.getDayOfMonth(), // 시작일의 '일'을 기준 결제일로 저장
                SubscriptionStatus.ACTIVE
        );
    }

    /**
     * 비즈니스 요구사항에 따라 다음과 같이 유연하게 생성 메서드를 추가할 수 있습니다.
     */
    public static Subscription startFreePromotionSubscription(BillingInfo billingInfo, String planName, Long amount, LocalDate startDate) {
        return new Subscription(
                billingInfo,
                planName,
                amount,
                startDate.plusMonths(2), // 첫 달 무료이므로 다음 결제일은 2달 뒤!
                startDate.getDayOfMonth(),
                SubscriptionStatus.ACTIVE
        );
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
        this.updatedAt = LocalDateTime.now();
    }

    // 구독 해지 요청 처리
    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
        this.updatedAt = LocalDateTime.now(); // 데이터 변경 시 업데이트 시간 갱신
    }

    // 결제 금액 검증 비즈니스 로직
    private void validateAmount(Long amount) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("정기 결제 금액은 0원 이상이어야 합니다.");
        }
    }
}