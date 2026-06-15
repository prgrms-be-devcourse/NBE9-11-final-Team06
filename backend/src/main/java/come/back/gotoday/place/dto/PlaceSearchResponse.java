package come.back.gotoday.place.dto;

import come.back.gotoday.external.naver.dto.NaverLocalItem;

public record PlaceSearchResponse(
        String name,
        String category,
        String address,
        String roadAddress,
        String phone,
        String placeUrl,
        String source
) {

    public static PlaceSearchResponse from(NaverLocalItem item) {
        return new PlaceSearchResponse(
                removeHtmlTags(item.title()),
                item.category(),
                item.address(),
                item.roadAddress(),
                item.telephone(),
                item.link(),
                "NAVER"
        );
    }

    private static String removeHtmlTags(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("<[^>]*>", "");
    }
}
