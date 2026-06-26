package come.back.gotoday.payment.billing.enums;

public enum BillingStatus {
    ACTIVE,   // 현재 결제에 사용 가능한 카드
    DELETED   // 사용자가 삭제한 카드 (이력 보존용)
}