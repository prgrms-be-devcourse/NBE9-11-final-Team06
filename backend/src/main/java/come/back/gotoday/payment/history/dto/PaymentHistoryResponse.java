package come.back.gotoday.payment.history.dto;

import come.back.gotoday.payment.history.entity.PaymentHistory;
import come.back.gotoday.payment.history.enums.PaymentStatus;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record PaymentHistoryResponse(
        Long paymentHistoryId,
        String orderId,
        Long amount,
        PaymentStatus status,
        String receiptUrl,
        String failureReason,
        LocalDateTime createdAt
) {
    public static PaymentHistoryResponse from(PaymentHistory history) {
        return PaymentHistoryResponse.builder()
                .paymentHistoryId(history.getId())
                .orderId(history.getOrderId())
                .amount(history.getAmount())
                .status(history.getStatus())
                .receiptUrl(history.getReceiptUrl())
                .failureReason(history.getFailureReason())
                .createdAt(history.getCreatedAt())
                .build();
    }
}