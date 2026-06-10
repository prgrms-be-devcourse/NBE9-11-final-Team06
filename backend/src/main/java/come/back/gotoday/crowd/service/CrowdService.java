package come.back.gotoday.crowd.service;

import come.back.gotoday.crowd.dto.CrowdResponse;
import come.back.gotoday.crowd.entity.CongestionLevel;
import come.back.gotoday.external.seoul.SeoulCrowdClient;
import come.back.gotoday.external.seoul.SeoulCrowdResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 혼잡도 조회 비즈니스 로직을 담당하는 서비스입니다.
 *
 * 서울시 실시간 도시데이터 API를 호출해 원본 응답을 받고,
 * 우리 서비스에서 사용하는 CrowdResponse 형태로 변환합니다.
 */
@Service
@Transactional(readOnly = true)
public class CrowdService {

    /** 서울시 API의 PPLTN_TIME 문자열 형식입니다. 예: 2026-06-10 15:10 */
    private static final DateTimeFormatter SEOUL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 서울시 실시간 도시데이터 API를 호출하는 외부 API 클라이언트입니다. */
    private final SeoulCrowdClient seoulCrowdClient;

    /**
     * SeoulCrowdClient를 생성자 주입 방식으로 주입받습니다.
     *
     * 생성자 주입을 사용하면 필수 의존성이 명확해지고 테스트 코드 작성이 쉬워집니다.
     */
    public CrowdService(SeoulCrowdClient seoulCrowdClient) {
        this.seoulCrowdClient = seoulCrowdClient;
    }

    /**
     * 지역명 기준으로 현재 혼잡도 정보를 조회합니다.
     *
     * 처리 흐름:
     * 1. 서울시 API 호출
     * 2. CITYDATA에서 장소명, 장소 코드, 실시간 인구현황 데이터 추출
     * 3. 한글 혼잡도 값을 CongestionLevel enum으로 변환
     * 4. 인구 범위와 측정 시각을 우리 서비스 타입에 맞게 변환
     * 5. CrowdResponse로 반환
     *
     * @param areaName 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소명
     * @return 클라이언트에 반환할 혼잡도 응답 DTO
     */
    public CrowdResponse getCrowdStatus(String areaName) {
        SeoulCrowdResponse response = seoulCrowdClient.getCrowdStatus(areaName);
        SeoulCrowdResponse.CityData cityData = response.CITYDATA();
        SeoulCrowdResponse.LivePopulationStatus populationStatus = getLatestPopulationStatus(cityData.LIVE_PPLTN_STTS());

        CongestionLevel congestionLevel = CongestionLevel.from(populationStatus.AREA_CONGEST_LVL());

        return new CrowdResponse(
                cityData.AREA_NM(),
                cityData.AREA_CD(),
                congestionLevel,
                congestionLevel.getText(),
                populationStatus.AREA_CONGEST_MSG(),
                parseInteger(populationStatus.AREA_PPLTN_MIN()),
                parseInteger(populationStatus.AREA_PPLTN_MAX()),
                parseDateTime(populationStatus.PPLTN_TIME())
        );
    }

    /**
     * 서울시 API 응답의 실시간 인구현황 목록에서 사용할 데이터를 꺼냅니다.
     *
     * 현재 API 응답에서는 LIVE_PPLTN_STTS 목록의 첫 번째 데이터에
     * 현재 혼잡도 정보가 들어있기 때문에 첫 번째 값을 사용합니다.
     *
     * @param populationStatuses 서울시 API의 실시간 인구현황 목록
     * @return 현재 혼잡도 정보로 사용할 실시간 인구현황 데이터
     */
    private SeoulCrowdResponse.LivePopulationStatus getLatestPopulationStatus(
            List<SeoulCrowdResponse.LivePopulationStatus> populationStatuses
    ) {
        if (populationStatuses == null || populationStatuses.isEmpty()) {
            throw new IllegalStateException("서울시 실시간 인구현황 데이터가 없습니다.");
        }

        return populationStatuses.get(0);
    }

    /**
     * 서울시 API에서 문자열로 내려주는 숫자 값을 Integer로 변환합니다.
     *
     * 값이 비어 있으면 null을 반환하고,
     * 숫자에 콤마가 포함된 경우 제거한 뒤 변환합니다.
     *
     * @param value 서울시 API에서 받은 숫자 문자열
     * @return 변환된 Integer 값
     */
    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Integer.parseInt(value.replace(",", ""));
    }

    /**
     * 서울시 API에서 문자열로 내려주는 날짜/시간 값을 LocalDateTime으로 변환합니다.
     *
     * 값이 비어 있으면 null을 반환합니다.
     *
     * @param value 서울시 API의 PPLTN_TIME 값
     * @return 변환된 LocalDateTime 값
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDateTime.parse(value, SEOUL_DATE_TIME_FORMATTER);
    }
}
