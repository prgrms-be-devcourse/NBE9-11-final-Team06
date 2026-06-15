package come.back.gotoday.place.dto;

import come.back.gotoday.external.naver.dto.NaverLocalItem;
import java.math.BigDecimal;
import java.util.regex.Pattern;

public record PlaceSearchResponse(
        String name,
        String category,
        String address,
        String roadAddress,
        String phone,
        String placeUrl,
        BigDecimal mapy,
        BigDecimal mapx,
        String source
) {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    public static PlaceSearchResponse from(NaverLocalItem item) {
        return new PlaceSearchResponse(
                removeHtmlTags(item.title()),
                item.category(),
                item.address(),
                item.roadAddress(),
                item.telephone(),
                item.link(),
                convertCoordinate(item.mapy()),
                convertCoordinate(item.mapx()),
                "NAVER"
        );
    }

    private static BigDecimal convertCoordinate(String coordinate) {
        if (coordinate == null || coordinate.isBlank()) {
            return null;
        }
        return new BigDecimal(coordinate);
    }

    private static String removeHtmlTags(String value) {
        if (value == null) {
            return null;
        }
        return HTML_TAG_PATTERN.matcher(value).replaceAll("");
    }
}
