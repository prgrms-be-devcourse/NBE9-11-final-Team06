
package come.back.gotoday.external.naver;

import tools.jackson.databind.JsonNode;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class NaverReverseGeocodingClient {

    private static final String REVERSE_GEOCODE_PATH = "/map-reversegeocode/v2/gc";

    private final RestClient restClient;
    private final NaverGeocodingProperties properties;

    public NaverReverseGeocodingClient(NaverGeocodingProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-NCP-APIGW-API-KEY-ID", properties.clientId())
                .defaultHeader("X-NCP-APIGW-API-KEY", properties.clientSecret())
                .requestFactory(requestFactory)
                .build();

        log.info(
                "Naver Reverse Geocoding API timeout 설정 완료: connectTimeoutSeconds={}, readTimeoutSeconds={}",
                properties.connectTimeoutSeconds(),
                properties.readTimeoutSeconds()
        );
    }

    /**
     * WGS84 좌표를 기준으로 행정구역 정보를 조회합니다.
     * 네이버 Reverse Geocoding API는 경도,위도 순서로 좌표를 받습니다.
     */
    public ReverseGeocodingResult reverseGeocode(double latitude, double longitude) {
        log.info("네이버 역지오코딩 요청: latitude={}, longitude={}", latitude, longitude);

        NaverReverseGeocodingApiResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(REVERSE_GEOCODE_PATH)
                            .queryParam("coords", longitude + "," + latitude)
                            .queryParam("sourcecrs", "epsg:4326")
                            .queryParam("orders", "admcode,addr,roadaddr")
                            .queryParam("output", "json")
                            .build())
                    .retrieve()
                    .body(NaverReverseGeocodingApiResponse.class);
        } catch (RestClientException exception) {
            log.warn(
                    "네이버 역지오코딩 API 호출 실패 또는 timeout 발생: latitude={}, longitude={}, exceptionType={}, message={}",
                    latitude,
                    longitude,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception
            );
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new IllegalStateException("네이버 역지오코딩 결과가 없습니다.");
        }

        NaverReverseGeocodingApiResponse.Result result = response.results().getFirst();
        NaverReverseGeocodingApiResponse.Region region = result.region();

        String district = region == null || region.area2() == null ? null : region.area2().name();
        String neighborhood = region == null || region.area3() == null ? null : region.area3().name();
        String resolvedName = joinAreaName(district, neighborhood);

        if (!StringUtils.hasText(resolvedName)) {
            throw new IllegalStateException("네이버 역지오코딩 응답에 구 또는 동 정보가 없습니다.");
        }

        log.info("네이버 역지오코딩 완료: areaName={}", resolvedName);
        return new ReverseGeocodingResult(resolvedName, district, neighborhood);
    }

    private String joinAreaName(String district, String neighborhood) {
        if (!StringUtils.hasText(district)) {
            return neighborhood;
        }

        if (!StringUtils.hasText(neighborhood)) {
            return district;
        }

        return district + " " + neighborhood;
    }

    public record ReverseGeocodingResult(
            String areaName,
            String district,
            String neighborhood
    ) {
    }

    private record NaverReverseGeocodingApiResponse(
            JsonNode status,
            List<Result> results
    ) {
        private record Result(
                String name,
                Region region
        ) {
        }

        private record Region(
                Area area1,
                Area area2,
                Area area3,
                Area area4
        ) {
        }

        private record Area(
                String name
        ) {
        }
    }
}
