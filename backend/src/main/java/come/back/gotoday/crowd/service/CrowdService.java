package come.back.gotoday.crowd.service;

import come.back.gotoday.crowd.dto.CrowdResponse;
import come.back.gotoday.crowd.entity.CongestionLevel;
import come.back.gotoday.crowd.entity.CrowdStatus;
import come.back.gotoday.crowd.repository.CrowdStatusRepository;
import come.back.gotoday.external.seoul.SeoulCrowdClient;
import come.back.gotoday.external.seoul.SeoulCrowdResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 혼잡도 조회 비즈니스 로직을 담당하는 서비스입니다.
 *
 * 서울시 실시간 도시데이터 API를 호출해 원본 응답을 받고,
 * 우리 서비스에서 사용하는 CrowdResponse 형태로 변환합니다.
 */
@Slf4j
@Service
public class CrowdService {

    /** DB에 저장된 혼잡도 데이터를 재사용할 캐시 유효 시간입니다. */
    private static final long CACHE_TTL_MINUTES = 5;

    /** 서울시 API의 PPLTN_TIME 문자열 형식입니다. 예: 2026-06-10 15:10 */
    private static final DateTimeFormatter SEOUL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 서울시 실시간 도시데이터 API를 호출하는 외부 API 클라이언트입니다. */
    private final SeoulCrowdClient seoulCrowdClient;

    /** 혼잡도 조회 결과를 저장하고 캐시 조회에 사용하는 Repository입니다. */
    private final CrowdStatusRepository crowdStatusRepository;

    /**
     * SeoulCrowdClient를 생성자 주입 방식으로 주입받습니다.
     *
     * 생성자 주입을 사용하면 필수 의존성이 명확해지고 테스트 코드 작성이 쉬워집니다.
     */
    public CrowdService(SeoulCrowdClient seoulCrowdClient, CrowdStatusRepository crowdStatusRepository) {
        this.seoulCrowdClient = seoulCrowdClient;
        this.crowdStatusRepository = crowdStatusRepository;
    }

    /**
     * 지역명 기준으로 현재 혼잡도 정보를 조회합니다.
     *
     * DB에 저장된 혼잡도 데이터가 캐시 유효 시간 이내이면 DB 데이터를 사용하고,
     * 그렇지 않으면 서울시 API에서 최신 데이터를 조회하여 DB에 저장 후 반환합니다.
     *
     * @param areaName 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소명
     * @return 클라이언트에 반환할 혼잡도 응답 DTO
     */
    public CrowdResponse getCrowdStatus(String areaName) {
        log.info("혼잡도 조회 처리 시작: areaName={}", areaName);
        Optional<CrowdStatus> cachedCrowdStatus = crowdStatusRepository.findTopByAreaNameOrderByCreatedAtDesc(areaName);

        if (cachedCrowdStatus.isPresent() && isFresh(cachedCrowdStatus.get())) {
            log.info("혼잡도 캐시 사용: areaName={}, crowdStatusId={}", areaName, cachedCrowdStatus.get().getId());
            return toResponse(cachedCrowdStatus.get());
        }

        try {
            log.info("혼잡도 최신 데이터 조회 시도: areaName={}", areaName);
            CrowdResponse response = fetchAndSaveCrowdStatus(areaName);
            log.info("혼잡도 최신 데이터 조회 완료: areaName={}", areaName);
            return response;
        } catch (RuntimeException exception) {
            log.warn("혼잡도 최신 데이터 조회 실패: areaName={}, message={}", areaName, exception.getMessage());

            if (cachedCrowdStatus.isPresent()) {
                log.info("혼잡도 최신 데이터 조회 실패로 기존 캐시 반환: areaName={}, crowdStatusId={}", areaName, cachedCrowdStatus.get().getId());
                return toResponse(cachedCrowdStatus.get());
            }

            log.error("혼잡도 조회 실패: 사용 가능한 캐시가 없습니다. areaName={}", areaName, exception);
            throw exception;
        }
    }

    /**
     * DB에 저장된 혼잡도 데이터가 캐시 유효 시간 이내인지 확인합니다.
     *
     * @param crowdStatus DB에 저장된 혼잡도 데이터
     * @return 저장 시각 기준 5분 이내이면 true
     */
    private boolean isFresh(CrowdStatus crowdStatus) {
        return crowdStatus.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(CACHE_TTL_MINUTES));
    }

    /**
     * 서울시 API에서 최신 혼잡도 데이터를 조회하고 DB에 저장한 뒤 응답으로 변환합니다.
     *
     * @param areaName 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소명
     * @return 클라이언트에 반환할 혼잡도 응답 DTO
     */
    private CrowdResponse fetchAndSaveCrowdStatus(String areaName) {
        log.info("서울시 혼잡도 API 조회 시작: areaName={}", areaName);
        SeoulCrowdResponse response = seoulCrowdClient.getCrowdStatus(areaName);
        log.info("서울시 혼잡도 API 조회 완료: areaName={}", areaName);
        SeoulCrowdResponse.CityData cityData = getCityData(response, areaName);
        SeoulCrowdResponse.LivePopulationStatus populationStatus = getLatestPopulationStatus(cityData.LIVE_PPLTN_STTS());

        CongestionLevel congestionLevel = CongestionLevel.from(populationStatus.AREA_CONGEST_LVL());
        log.info("혼잡도 등급 변환 완료: areaName={}, congestionLevel={}", areaName, congestionLevel);
        Integer populationMin = parseInteger(populationStatus.AREA_PPLTN_MIN());
        Integer populationMax = parseInteger(populationStatus.AREA_PPLTN_MAX());
        LocalDateTime measuredAt = parseDateTime(populationStatus.PPLTN_TIME());

        CrowdStatus crowdStatus = CrowdStatus.create(
                null,
                cityData.AREA_NM(),
                cityData.AREA_CD(),
                congestionLevel,
                populationMin,
                populationMax,
                populationStatus.AREA_CONGEST_MSG(),
                measuredAt
        );

        CrowdStatus savedCrowdStatus = crowdStatusRepository.save(crowdStatus);
        log.info("혼잡도 데이터 저장 완료: crowdStatusId={}, areaName={}, measuredAt={}", savedCrowdStatus.getId(), savedCrowdStatus.getAreaName(), savedCrowdStatus.getMeasuredAt());

        return toResponse(savedCrowdStatus);
    }

    /**
     * DB에 저장된 혼잡도 엔티티를 API 응답 DTO로 변환합니다.
     *
     * @param crowdStatus DB에 저장된 혼잡도 데이터
     * @return 클라이언트에 반환할 혼잡도 응답 DTO
     */
    private CrowdResponse toResponse(CrowdStatus crowdStatus) {
        CongestionLevel congestionLevel = crowdStatus.getCongestionLevel();

        return new CrowdResponse(
                crowdStatus.getAreaName(),
                crowdStatus.getAreaCode(),
                congestionLevel,
                congestionLevel.getText(),
                crowdStatus.getMessage(),
                crowdStatus.getPopulationMin(),
                crowdStatus.getPopulationMax(),
                crowdStatus.getMeasuredAt()
        );
    }

    /**
     * 서울시 API 응답에서 CITYDATA 영역을 안전하게 꺼냅니다.
     *
     * API 키 오류, 잘못된 지역명, 외부 API 응답 실패 등으로 CITYDATA가 없을 수 있으므로
     * null 여부를 확인한 뒤 명확한 예외 메시지를 반환합니다.
     *
     * @param response 서울시 API 원본 응답
     * @param areaName 요청한 핫스팟 장소명
     * @return 서울시 API의 CITYDATA 영역
     */
    private SeoulCrowdResponse.CityData getCityData(SeoulCrowdResponse response, String areaName) {
        if (response == null || response.CITYDATA() == null) {
            log.warn("서울시 혼잡도 API 응답에 CITYDATA가 없습니다. areaName={}", areaName);
            throw new BusinessException(ErrorCode.CROWD_AREA_NOT_FOUND);
        }

        return response.CITYDATA();
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
            log.warn("서울시 실시간 인구현황 데이터가 없습니다.");
            throw new BusinessException(ErrorCode.CROWD_DATA_NOT_FOUND);
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
            log.debug("혼잡도 인구 수 파싱 스킵: value={}", value);
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
            log.debug("혼잡도 측정 시각 파싱 스킵: value={}", value);
            return null;
        }

        return LocalDateTime.parse(value, SEOUL_DATE_TIME_FORMATTER);
    }
}
