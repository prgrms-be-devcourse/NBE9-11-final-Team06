package come.back.gotoday.payment.subscription.enums;

public enum SubscriptionStatus {
    ACTIVE,   // 구독 중 및 자동결제 활성화
    PAUSED,   // 일시 정지 (결제 건너뜀)
    CANCELED  // 해지됨 (자동결제 대상에서 영구 제외)
}
