package come.back.gotoday.crowd.dto;

import come.back.gotoday.crowd.entity.CongestionLevel;
import java.time.LocalDateTime;

/**
 * 혼잡도 조회 API의 응답 DTO입니다.
 *
 * 서울시 실시간 도시데이터 API에서 받은 원본 응답을
 * 우리 서비스에서 사용하기 쉬운 형태로 변환해 클라이언트에 반환합니다.
 */
public record CrowdResponse(
        /** 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소명 */
        String areaName,

        /** 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 코드 */
        String areaCode,

        /** 추천 점수 계산에 사용할 수 있도록 enum으로 변환한 혼잡도 단계 */
        CongestionLevel congestionLevel,

        /** 사용자에게 보여줄 한글 혼잡도 텍스트 */
        String congestionText,

        /** 서울시 API에서 제공하는 혼잡도 안내 메시지 */
        String message,

        /** 실시간 인구 지표 최소값 */
        Integer populationMin,

        /** 실시간 인구 지표 최대값 */
        Integer populationMax,

        /** 혼잡도 데이터가 측정 또는 업데이트된 시각 */
        LocalDateTime measuredAt
) {
}
