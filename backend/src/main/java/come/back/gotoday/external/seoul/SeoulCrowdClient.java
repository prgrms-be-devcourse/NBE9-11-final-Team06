package come.back.gotoday.external.seoul;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * 서울시 실시간 도시데이터 API를 호출하는 외부 API 클라이언트입니다.
 *
 * CrowdService에서 지역명(areaName)을 전달하면,
 * 서울시 API 요청 URL을 생성하고 RestClient로 호출한 뒤
 * SeoulCrowdResponse 형태로 응답을 반환합니다.
 */
@Slf4j
@Component
public class SeoulCrowdClient {

    /** 서울시 API 응답 형식입니다. 현재는 JSON으로 응답을 받습니다. */
    private static final String RESPONSE_TYPE = "json";

    /** 서울시 실시간 도시데이터 API의 서비스명입니다. */
    private static final String SERVICE_NAME = "citydata";

    /** 서울시 API 조회 시작 인덱스입니다. */
    private static final int START_INDEX = 1;

    /** 서울시 API 조회 종료 인덱스입니다. */
    private static final int END_INDEX = 5;

    /** HTTP 요청을 보내기 위한 Spring RestClient입니다. */
    private final RestClient restClient;

    /** 서울시 API 기본 URL과 인증키 설정 값입니다. */
    private final SeoulApiProperties seoulApiProperties;

    /**
     * 서울시 API 설정 값을 주입받고 RestClient를 생성합니다.
     *
     * Spring Boot가 제공하는 RestClient.Builder가 있으면 해당 Builder를 사용하고,
     * 없으면 기본 Builder를 사용해 외부 API 호출용 클라이언트를 생성합니다.
     */
    public SeoulCrowdClient(
            SeoulApiProperties seoulApiProperties,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider
    ) {
        RestClient.Builder restClientBuilder = restClientBuilderProvider.getIfAvailable(RestClient::builder);
        this.restClient = restClientBuilder.build();
        this.seoulApiProperties = seoulApiProperties;
    }

    /**
     * 서울시 실시간 도시데이터 API에서 제공하는 전체 혼잡도 대상 지역명을 반환합니다.
     *
     * 전체 지역 혼잡도 이력을 정기 수집할 때 사용합니다.
     *
     * @return 서울시 혼잡도 대상 전체 지역명 목록
     */
    public List<String> getAllAreaNames() {
        return SeoulCrowdArea.getAllAreaNames();
    }

    /**
     * 지역명 기준으로 서울시 실시간 혼잡도 정보를 조회합니다.
     *
     * 생성되는 요청 URL 예시:
     * http://openapi.seoul.go.kr:8088/{API_KEY}/json/citydata/1/5/성수카페거리
     *
     * @param areaName 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소명
     * @return 서울시 API 원본 응답을 매핑한 DTO
     */
    public SeoulCrowdResponse getCrowdStatus(String areaName) {
        log.info("서울시 실시간 도시데이터 API 요청 URL 생성 시작: areaName={}, startIndex={}, endIndex={}", areaName, START_INDEX, END_INDEX);
        String url = UriComponentsBuilder.fromUriString(seoulApiProperties.baseUrl())
                .pathSegment(
                        seoulApiProperties.apiKey(),
                        RESPONSE_TYPE,
                        SERVICE_NAME,
                        String.valueOf(START_INDEX),
                        String.valueOf(END_INDEX),
                        areaName
                )
                .build()
                .encode()
                .toUriString();
        log.info("서울시 실시간 도시데이터 API 요청 URL 생성 완료: areaName={}", areaName);

        try {
            log.info("서울시 실시간 도시데이터 API 호출 시작: areaName={}", areaName);
            SeoulCrowdResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(SeoulCrowdResponse.class);

            if (response == null) {
                log.warn("서울시 실시간 도시데이터 API 응답이 비어 있습니다. areaName={}", areaName);
                throw new BusinessException(ErrorCode.CROWD_API_RESPONSE_EMPTY);
            }

            log.info("서울시 실시간 도시데이터 API 호출 완료: areaName={}, hasCityData={}", areaName, response.CITYDATA() != null);
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.error("서울시 실시간 도시데이터 API 호출 실패: areaName={}, message={}", areaName, exception.getMessage(), exception);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }
}
