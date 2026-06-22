package come.back.gotoday.external.seoul;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class SeoulCrowdClient {

    private static final String RESPONSE_TYPE = "json";
    private static final String SERVICE_NAME = "citydata";
    private static final int START_INDEX = 1;
    private static final int END_INDEX = 5;

    private final RestClient restClient;
    private final SeoulApiProperties seoulApiProperties;
    private final SeoulCrowdArea seoulCrowdArea;

    public SeoulCrowdClient(
            SeoulApiProperties seoulApiProperties,
            SeoulCrowdArea seoulCrowdArea,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider
    ) {
        RestClient.Builder restClientBuilder = restClientBuilderProvider.getIfAvailable(RestClient::builder);
        this.restClient = restClientBuilder.build();
        this.seoulApiProperties = seoulApiProperties;
        this.seoulCrowdArea = seoulCrowdArea;
    }

    public List<String> getAllAreaNames() {
        return seoulCrowdArea.getAllAreaNames();
    }

    public SeoulCrowdResponse getCrowdStatus(String areaName) {
        log.info("서울시 실시간 도시데이터 API 요청 URL 생성 시작: areaName={}, startIndex={}, endIndex={}",
                areaName, START_INDEX, END_INDEX);

        URI uri = UriComponentsBuilder.fromUriString(seoulApiProperties.baseUrl())
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
                .toUri();

        log.info("서울시 실시간 도시데이터 API 요청 URL 생성 완료: areaName={}", areaName);

        try {
            log.info("서울시 실시간 도시데이터 API 호출 시작: areaName={}", areaName);

            SeoulCrowdResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(SeoulCrowdResponse.class);

            if (response == null) {
                log.warn("서울시 실시간 도시데이터 API 응답이 비어 있습니다. areaName={}", areaName);
                throw new BusinessException(ErrorCode.CROWD_API_RESPONSE_EMPTY);
            }

            log.info("서울시 실시간 도시데이터 API 호출 완료: areaName={}, hasCityData={}",
                    areaName, response.CITYDATA() != null);

            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.error("서울시 실시간 도시데이터 API 호출 실패: areaName={}, message={}",
                    areaName, exception.getMessage(), exception);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }
}