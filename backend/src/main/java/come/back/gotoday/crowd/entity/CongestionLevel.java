package come.back.gotoday.crowd.entity;

public enum CongestionLevel {
    RELAXED("여유"),
    NORMAL("보통"),
    CROWDED("약간 붐빔"),
    VERY_CROWDED("붐빔");

    private final String text;

    CongestionLevel(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

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
