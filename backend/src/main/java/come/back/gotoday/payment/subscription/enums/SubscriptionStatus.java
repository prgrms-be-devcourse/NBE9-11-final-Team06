package come.back.gotoday.payment.subscription.enums;

public enum SubscriptionStatus {
    PENDING,  // 결제 승인 대기 중 (최초 구독 신청 시)
    ACTIVE,   // 구독 중 및 자동결제 활성화
    PAUSED,   // 일시 정지 (결제 건너뜀)
    CANCELED_RESERVED, // 구독 해지 예약 (이번 달까지 이용 후 해지)
    CANCELED, // 해지됨 (자동결제 대상에서 영구 제외)
    MANUAL_CHECK, // 정산 불일치 건을 추적하기 위해 추가
    EXPIRED_PAYMENT_PENDING // 자동 결제할때 실패할 경우
}
