package come.back.gotoday.payment.idempotency.enums;

public enum IdempotencyStatus {
    PROCESSING, // 첫 요청을 받아 처리 중인 상태 (중복 진입 차단용)
    SUCCESS,    // 처리가 성공적으로 완료되어 응답값이 저장된 상태
    FAILED,      // 처리가 실패로 끝나 재시도가 가능한 상태
    TIMEOUT     // 네트워크 타임아웃 발생 (동일 멱등키로 재시도/구출 허용)
}