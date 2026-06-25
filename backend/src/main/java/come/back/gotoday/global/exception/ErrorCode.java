package come.back.gotoday.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),

    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN", "이메일 또는 비밀번호가 올바르지 않습니다."),
    OAUTH_MEMBER_CANNOT_LOGIN_WITH_PASSWORD(
            HttpStatus.BAD_REQUEST,
            "OAUTH_MEMBER_CANNOT_LOGIN_WITH_PASSWORD",
            "소셜 로그인으로 가입한 회원은 비밀번호 로그인을 사용할 수 없습니다."
    ),

    DUPLICATE_PREFERENCE(HttpStatus.CONFLICT, "DUPLICATE_PREFERENCE", "이미 등록된 선호 정보가 있습니다."),
    PREFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "PREFERENCE_NOT_FOUND", "등록된 선호 정보가 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."),

    PLACE_ALREADY_EXISTS(HttpStatus.CONFLICT, "PLACE_ALREADY_EXISTS", "이미 등록된 장소입니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE_NOT_FOUND", "장소를 찾을 수 없습니다."),

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "코스를 찾을 수 없습니다."),
    RECOMMENDATION_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RECOMMENDATION_EVENT_NOT_FOUND", "추천 가능한 행사가 없습니다."),
    COURSE_ALREADY_BOOKMARKED(HttpStatus.CONFLICT, "COURSE_ALREADY_BOOKMARKED", "이미 북마크한 코스입니다."),

    CROWD_AREA_NOT_FOUND(HttpStatus.NOT_FOUND, "CROWD_AREA_NOT_FOUND", "혼잡도 정보를 조회할 지역을 찾을 수 없습니다."),
    CROWD_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "CROWD_DATA_NOT_FOUND", "혼잡도 데이터를 찾을 수 없습니다."),
    CROWD_API_RESPONSE_EMPTY(HttpStatus.BAD_GATEWAY, "CROWD_API_RESPONSE_EMPTY", "서울시 혼잡도 API 응답이 비어 있습니다."),
    CROWD_API_PARSE_FAILED(HttpStatus.BAD_GATEWAY, "CROWD_API_PARSE_FAILED", "서울시 혼잡도 API 응답 처리 중 오류가 발생했습니다."),

    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", "외부 API 서버와의 통신 중 오류가 발생했습니다."),
    EXTERNAL_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "EXTERNAL_API_TIMEOUT", "외부 API 서버 응답 시간이 초과되었습니다."),

    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."),



    // --- 요금제 관련 에러 코드 ---
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "요금제를 찾을 수 없습니다."),

    // --- 구독 및 결제 관련 에러 코드 ---
    INVALID_BILLING_INFO(HttpStatus.BAD_REQUEST, "INVALID_BILLING_INFO", "유효하지 않은 결제 정보입니다."),
    DUPLICATE_ACTIVE_SUBSCRIPTION(HttpStatus.CONFLICT, "DUPLICATE_ACTIVE_SUBSCRIPTION", "이미 활성화된 구독이 존재합니다."),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다."),
    ACTIVE_SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "ACTIVE_SUBSCRIPTION_NOT_FOUND", "현재 활성화된 구독을 찾을 수 없습니다."),
    UNAUTHORIZED_SUBSCRIPTION_ACCESS(HttpStatus.FORBIDDEN, "UNAUTHORIZED_SUBSCRIPTION_ACCESS", "해당 구독 정보에 대한 접근 권한이 없습니다."),
    PAYMENT_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_HISTORY_NOT_FOUND", "결제 내역을 찾을 수 없습니다."),
    CANNOT_CANCEL_FAILED_PAYMENT(HttpStatus.BAD_REQUEST, "CANNOT_CANCEL_FAILED_PAYMENT", "실패한 결제는 취소할 수 없습니다."),

    // --- 주문 아이디 관련 에러 코드 ---
    INVALID_ORDER_ID_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_ORDER_ID_FORMAT", "주문 ID 형식이 올바르지 않습니다."),


    ALREADY_PROCESSED_PAYMENT(HttpStatus.BAD_REQUEST,"ALREADY_PROCESSED_PAYMENT","이미 진행중인 결제입니다"),

    // 400 Bad Request 관련
    INVALID_CARD_NUMBER(HttpStatus.BAD_REQUEST, "INVALID_CARD_NUMBER", "카드번호를 다시 확인해주세요."),
    NOT_SUPPORTED_CARD_TYPE(HttpStatus.BAD_REQUEST, "NOT_SUPPORTED_CARD_TYPE", "지원되지 않는 카드 종류입니다."),
    INVALID_CARD_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_CARD_PASSWORD", "카드 정보를 다시 확인해주세요. (비밀번호)"),
    INVALID_CARD_EXPIRATION(HttpStatus.BAD_REQUEST, "INVALID_CARD_EXPIRATION", "카드 정보를 다시 확인해주세요. (유효기간)"),
    INVALID_CARD_IDENTITY(HttpStatus.BAD_REQUEST, "INVALID_CARD_IDENTITY", "입력하신 주민번호/사업자번호가 카드 소유주 정보와 일치하지 않습니다."),
    INVALID_REJECT_CARD(HttpStatus.BAD_REQUEST, "INVALID_REJECT_CARD", "카드 사용이 거절되었습니다. 카드사 문의가 필요합니다."),
    INVALID_STOPPED_CARD(HttpStatus.BAD_REQUEST, "INVALID_STOPPED_CARD", "정지된 카드 입니다."),
    INVALID_BIRTH_DAY_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_BIRTH_DAY_FORMAT", "생년월일 정보는 6자리의 yyMMdd 형식이어야 합니다. 사업자등록번호는 10자리의 숫자여야 합니다."),
    NOT_REGISTERED_CARD_COMPANY(HttpStatus.BAD_REQUEST, "NOT_REGISTERED_CARD_COMPANY", "카드를 사용 등록 후 이용해주세요."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "유효하지 않은 이메일 주소 형식입니다."),
    NOT_SUPPORTED_METHOD(HttpStatus.BAD_REQUEST, "NOT_SUPPORTED_METHOD", "지원되지 않는 결제 수단입니다."),
    // INVALID_REQUEST는 상단에 이미 존재하므로 공통 사용

    // 403 Forbidden 관련
    EXCEED_MAX_AUTH_COUNT(HttpStatus.FORBIDDEN, "EXCEED_MAX_AUTH_COUNT", "최대 인증 횟수를 초과했습니다. 카드사로 문의해주세요."),
    REJECT_CARD_COMPANY(HttpStatus.FORBIDDEN, "REJECT_CARD_COMPANY", "결제 승인이 거절되었습니다."),
    REJECT_ACCOUNT_PAYMENT(HttpStatus.FORBIDDEN, "REJECT_ACCOUNT_PAYMENT", "잔액부족으로 결제에 실패했습니다."),
    FORBIDDEN_REQUEST(HttpStatus.FORBIDDEN, "FORBIDDEN_REQUEST", "허용되지 않은 요청입니다."),

    // 500 Internal Server Error 관련
    COMMON_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_ERROR", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    NETWORK_ERROR_FINAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NETWORK_ERROR_FINAL_FAILED", "결제 시스템과의 통신이 원활하지 않아 요청이 최종 실패했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}