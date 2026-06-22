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

    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다.");

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