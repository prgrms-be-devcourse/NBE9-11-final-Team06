
package come.back.gotoday.external.weather;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 기상청 단기예보 조회서비스 API를 호출하는 외부 연동 클라이언트입니다.
 *
 * 이 클래스는 API 요청과 원본 응답 수신만 담당합니다.
 * 예보 항목 선택, 날씨 상태 분류, 추천 점수 계산은 WeatherForecastService에서 처리합니다.
 */
@Slf4j
@Component
public class KmaWeatherClient {

    private static final String DEFAULT_BASE_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";

    private final RestClient restClient;
    private final String serviceKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KmaWeatherClient(
            @Value("${weather.kma.service-key}") String serviceKey,
            @Value("${weather.kma.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.serviceKey = serviceKey;
    }

    /**
     * 기상청 단기예보를 조회합니다.
     *
     * @param baseDate 발표일자, yyyyMMdd 형식
     * @param baseTime 발표시각, HHmm 형식
     * @param nx 기상청 격자 X 좌표
     * @param ny 기상청 격자 Y 좌표
     * @return 기상청 원본 JSON 응답. 호출 실패 또는 응답이 없으면 null
     */
    public JsonNode getVillageForecast(
            String baseDate,
            String baseTime,
            int nx,
            int ny
    ) {
        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 1000)
                            .queryParam("dataType", "JSON")
                            .queryParam("base_date", baseDate)
                            .queryParam("base_time", baseTime)
                            .queryParam("nx", nx)
                            .queryParam("ny", ny)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            JsonNode response = responseBody == null
                    ? null
                    : objectMapper.readTree(responseBody);

            if (response == null) {
                log.warn(
                        "기상청 단기예보 응답이 비어 있습니다. baseDate={}, baseTime={}, nx={}, ny={}",
                        baseDate,
                        baseTime,
                        nx,
                        ny
                );
            }

            return response;
        } catch (RestClientException | JsonProcessingException exception) {
            log.warn(
                    "기상청 단기예보 조회에 실패했습니다. baseDate={}, baseTime={}, nx={}, ny={}",
                    baseDate,
                    baseTime,
                    nx,
                    ny,
                    exception
            );
            return null;
        }
    }
}
