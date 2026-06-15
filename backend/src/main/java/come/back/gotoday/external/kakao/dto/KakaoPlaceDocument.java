package come.back.gotoday.external.kakao.dto;

public record KakaoPlaceDocument(
        String place_name,
        String address_name,
        String road_address_name,
        String phone,
        String place_url,
        String category_name,
        String distance,
        String x, // 경도
        String y  // 위도
) {
}