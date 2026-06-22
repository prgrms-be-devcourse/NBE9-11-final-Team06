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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status; // 구독 상태 (ACTIVE, PAUSED, CANCELED)

    // 직접 추가된 생성 시간 및 업데이트 시간 필드
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 내부 생성자
    private Subscription(BillingInfo billingInfo, String planName, Long amount, LocalDate nextBillingDate, SubscriptionStatus status) {
        this.billingInfo = billingInfo;
        this.planName = planName;
        this.amount = amount;
        this.nextBillingDate = nextBillingDate;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Subscription startSubscription(BillingInfo billingInfo, String planName, Long amount) {
        return new Subscription(
                billingInfo,
                planName,
                amount,
                LocalDate.now().plusMonths(1), // 최초 생성 시 다음 결제일은 대개 한 달 뒤
                SubscriptionStatus.ACTIVE
        );
    }

    // 결제 성공 시 다음 결제일 갱신
    public void renewNextBillingDate() {
        this.nextBillingDate = this.nextBillingDate.plusMonths(1);
        this.updatedAt = LocalDateTime.now(); // 데이터 변경 시 업데이트 시간 갱신
    }

    // 구독 해지 요청 처리
    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
        this.updatedAt = LocalDateTime.now(); // 데이터 변경 시 업데이트 시간 갱신
    }
}