package come.back.gotoday.course.type;

public enum RestaurantType {
    KOREAN("한식"),
    JAPANESE("일식"),
    CHINESE("중식"),
    WESTERN("양식"),
    NOTHING("선택안함");

    private final String keyword;

    RestaurantType(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }
}
