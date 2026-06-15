package come.back.gotoday.external.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoPlaceDocument(
        @JsonProperty("place_name")
        String placeName,
        @JsonProperty("address_name")
        String addressName,
        @JsonProperty("road_address_name")
        String roadAddressName,
        String phone,
        @JsonProperty("place_url")
        String placeUrl,
        @JsonProperty("category_name")
        String categoryName,
        String distance,
        String x, // 경도
        String y  // 위도
) {
}