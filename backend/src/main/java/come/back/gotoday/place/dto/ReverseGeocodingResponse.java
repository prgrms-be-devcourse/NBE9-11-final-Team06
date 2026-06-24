package come.back.gotoday.place.dto;

/**
 * 좌표를 역지오코딩해 조회한 행정구역 정보입니다.
 */
public record ReverseGeocodingResponse(
        String areaName,
        String district,
        String neighborhood
) {
}
