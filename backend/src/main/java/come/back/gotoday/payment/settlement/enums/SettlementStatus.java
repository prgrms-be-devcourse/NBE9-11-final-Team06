package come.back.gotoday.payment.settlement.enums;

public enum SettlementStatus {
    MATCHED,               // 대조 성공 (정상 결제/정상 취소)
    MISMATCHED_AMOUNT,     // 금액 불일치 (우리 DB 금액 != 토스 정산 금액)
    MISMATCHED_STATUS,     // 상태 불일치 (우리 DB는 SUCCESS인데 토스는 취소 건으로 옴 등)
    NOT_FOUND_PAYMENT      // 우리 DB에 해당 결제 내역(PaymentHistory)이 없음
}