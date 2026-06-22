package come.back.gotoday.payment.subscription.entity;

import come.back.gotoday.payment.subscription.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentHistory { // BaseEntity 상속 제거

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //어떤 구독 건에 의해 발생한 결제 이력인가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId; // 가맹점에서 자체 생성한 고유 주문 ID

    @Column(nullable = false)
    private Long amount; // 실제 승인 요청 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status; // 결제 상태 (SUCCESS, FAILED)

    @Column(name = "failure_reason", length = 500)
    private String failureReason; // 결제 실패 시 사유 (잔액 부족 등)

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl; // 토스 리턴 전자 영수증 URL

    // 직접 추가된 생성 시간 및 업데이트 시간 필드
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 내부 생성자
    private PaymentHistory(Subscription subscription, String orderId, Long amount, PaymentStatus status, String failureReason, String receiptUrl) {
        this.subscription = subscription;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
        this.receiptUrl = receiptUrl;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 1: 결제 성공 이력 생성
    public static PaymentHistory createSuccessHistory(Subscription subscription, String orderId, Long amount, String receiptUrl) {
        return new PaymentHistory(subscription, orderId, amount, PaymentStatus.SUCCESS, null, receiptUrl);
    }

    // 2: 결제 실패 이력 생성
    public static PaymentHistory createFailureHistory(Subscription subscription, String orderId, Long amount, String failureReason) {
        return new PaymentHistory(subscription, orderId, amount, PaymentStatus.FAILED, failureReason, null);
    }
}