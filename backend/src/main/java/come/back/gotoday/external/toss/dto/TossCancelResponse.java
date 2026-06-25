package come.back.gotoday.external.toss.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossCancelResponse(
        String paymentKey,
        String orderId,
        String orderName,
        String status, // "CANCELED"
        Long totalAmount,
        String requestedAt,
        String approvedAt,
        List<CancelDetail> cancels,
        ReceiptInfo receipt
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CancelDetail(
            String transactionKey,
            String cancelReason,
            Long cancelAmount,
            String canceledAt,
            String cancelStatus // "DONE"
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReceiptInfo(
            String url
    ) {}
}