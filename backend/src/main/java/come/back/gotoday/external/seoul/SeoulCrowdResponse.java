package come.back.gotoday.external.seoul;

import java.util.List;

/**
 * 서울시 실시간 도시데이터 API의 원본 응답을 매핑하는 DTO입니다.
 *
 * 서울시 API는 응답 필드명이 대문자 스네이크 케이스 형태이므로,
 * 별도의 @JsonProperty 없이 자동 매핑되도록 API 응답 필드명과 동일하게 작성합니다.
 */
public record SeoulCrowdResponse(
        /** 서울시 API 응답의 최상위 데이터 영역입니다. */
        CityData CITYDATA
) {

    /**
     * CITYDATA 영역의 주요 데이터입니다.
     *
     * 현재 혼잡도 조회 기능에서는 장소명, 장소 코드, 실시간 인구현황 목록만 사용합니다.
     */
    public record CityData(
            /** 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소명입니다. */
            String AREA_NM,

            /** 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 코드입니다. 예: POI068 */
            String AREA_CD,

            /** 해당 핫스팟의 위도입니다. */
            String LAT,

            /** 해당 핫스팟의 경도입니다. */
            String LNG,

            /** 해당 핫스팟의 실시간 인구현황 목록입니다. */
            List<LivePopulationStatus> LIVE_PPLTN_STTS
    ) {
    }

    /**
     * 실시간 인구현황 데이터입니다.
     *
     * 장소 혼잡도, 혼잡도 안내 메시지, 예상 인구 범위,
     * 데이터 업데이트 시각을 담고 있습니다.
     */
    public record LivePopulationStatus(
            /** 서울시 API에서 제공하는 장소 혼잡도 지표입니다. 예: 여유, 보통, 약간 붐빔, 붐빔 */
            String AREA_CONGEST_LVL,

            /** 혼잡도 지표와 관련된 사용자 안내 메시지입니다. */
            String AREA_CONGEST_MSG,

            /** 실시간 인구 지표 최소값입니다. 문자열로 내려오므로 서비스에서 Integer로 변환합니다. */
            String AREA_PPLTN_MIN,

            /** 실시간 인구 지표 최대값입니다. 문자열로 내려오므로 서비스에서 Integer로 변환합니다. */
            String AREA_PPLTN_MAX,

            /** 실시간 인구 데이터 업데이트 시간입니다. 서비스에서 LocalDateTime으로 변환합니다. */
            String PPLTN_TIME
    ) {
    }
}