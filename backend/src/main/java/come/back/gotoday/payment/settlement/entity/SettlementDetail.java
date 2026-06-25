package come.back.gotoday.payment.settlement.entity;

import come.back.gotoday.payment.settlement.enums.SettlementStatus;
import come.back.gotoday.payment.subscription.entity.PaymentHistory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_detail", indexes = {
        @Index(name = "idx_settlement_date", columnList = "settlement_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 결제가 우리 DB에 없을 수도 있으므로 nullable = true 설정 (NOT_FOUND_PAYMENT 케이스 대처)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_history_id", nullable = true)
    private PaymentHistory paymentHistory;

    @Column(name = "order_id", nullable = false)
    private String orderId; // 대조 기준이 되는 가맹점 고유 주문 ID

    @Column(name = "payment_key", nullable = false)
    private String paymentKey;

    @Column(nullable = false)
    private Long amount; // 토스에서 보낸 거래 금액 (취소는 음수)

    @Column(nullable = false)
    private Long fee; // 토스 수수료 (취소는 음수)

    @Column(nullable = false)
    private Long vat; // 토스가 계산한 부가세 (취소는 음수)

    @Column(name = "pay_out_amount", nullable = false)
    private Long payOutAmount; // 실제 회사 통장에 입금/차감되는 금액

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate; // 실제 정산 지급일 (paidOutDate)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SettlementStatus status; // [변경] 정산 전용 상태값 관리

    @Column(name = "description", length = 500)
    private String description; // 불일치 사유 상세 기록용

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private SettlementDetail(PaymentHistory paymentHistory, String orderId, String paymentKey, Long amount, Long fee, Long vat, Long payOutAmount, LocalDate settlementDate, SettlementStatus status, String description) {
        this.paymentHistory = paymentHistory;
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.fee = fee;
        this.vat = vat;
        this.payOutAmount = payOutAmount;
        this.settlementDate = settlementDate;
        this.status = status;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public static SettlementDetail create(PaymentHistory paymentHistory, String orderId, String paymentKey, Long amount, Long fee, Long vat, Long payOutAmount, LocalDate settlementDate, SettlementStatus status, String description) {
        return new SettlementDetail(paymentHistory, orderId, paymentKey, amount, fee, vat, payOutAmount, settlementDate, status, description);
    }

}