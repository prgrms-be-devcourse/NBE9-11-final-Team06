package come.back.gotoday.crowd.entity;

/**
 * 서울시 실시간 도시데이터 API에서 제공하는 혼잡도 값을
 * 우리 서비스에서 사용하기 쉬운 enum 값으로 관리합니다.
 *
 * 문자열 그대로 사용하지 않고 enum으로 관리하면
 * 추천 점수 계산, 조건 분기, 응답 값 관리가 더 안전해집니다.
 */
public enum CongestionLevel {
    /** 사람이 적고 비교적 쾌적한 상태 */
    RELAXED("여유"),

    /** 사람이 보통 수준으로 있는 상태 */
    NORMAL("보통"),

    /** 사람이 많아 다소 붐비는 상태 */
    CROWDED("약간 붐빔"),

    /** 사람이 매우 많아 혼잡한 상태 */
    VERY_CROWDED("붐빔");

    /** 클라이언트에게 보여줄 한글 혼잡도 문구 */
    private final String text;

    CongestionLevel(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    /**
     * 서울시 API에서 내려주는 한글 혼잡도 값을 우리 서비스의 enum 값으로 변환합니다.
     *
     * 예상하지 못한 값이 들어오더라도 서비스가 바로 실패하지 않도록
     * 기본값은 NORMAL로 처리합니다.
     *
     * @param value 서울시 API의 혼잡도 값
     * @return 우리 서비스에서 사용하는 혼잡도 enum 값
     */
    public static CongestionLevel from(String value) {
        return switch (value) {
            case "여유" -> RELAXED;
            case "보통" -> NORMAL;
            case "약간 붐빔" -> CROWDED;
            case "붐빔" -> VERY_CROWDED;
            default -> NORMAL;
        };
    }
}
