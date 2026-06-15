package come.back.gotoday.external.kakao.dto;

import lombok.Getter;

@Getter
public class KakaoPlaceDocument {

    private String place_name;

    private String address_name;

    private String road_address_name;

    private String phone;

    private String place_url;

    private String category_name;

    private String distance;

    private String x; // 경도

    private String y; // 위도
}