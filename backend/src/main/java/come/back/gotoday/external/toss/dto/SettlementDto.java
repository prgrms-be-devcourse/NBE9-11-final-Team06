package come.back.gotoday.external.toss.dto;

import java.time.LocalDate;

public class SettlementDto {

    // 토스페이먼츠 정산 API 응답 매핑용 Record
    public record TossSettlementResponse(
            String orderId,
            String paymentKey,
            Long amount,          // 거래 금액 (취소는 음수)
            Long fee,             // 수수료 (취소는 음수)
            Long vat,             // 부가세 (취소는 음수)
            Long payOutAmount,    // 실제 정산 금액 (취소는 음수)
            LocalDate paidOutDate,// 정산 지급일
            CancelInfo cancel     // 취소 정보 객체 (취소 건이 아니면 null)
    ) {}

    public record CancelInfo(
            String cancelReason,
            Long cancelAmount,
            String cancelStatus
    ) {}
}
