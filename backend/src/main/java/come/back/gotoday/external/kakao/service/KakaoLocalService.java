package come.back.gotoday.external.kakao.service;

import come.back.gotoday.course.type.RestaurantType;
import come.back.gotoday.external.kakao.dto.KakaoPlaceResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@Slf4j
@Service
public class KakaoLocalService {

    private final RestClient restClient;

    @Value("${external.kakao.base-url:https://dapi.kakao.com}")
    private String baseUrl;

    @Value("${external.kakao.rest-api-key:dummy-key}")
    private String apiKey;

    public KakaoLocalService(
            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
            @Value("${external.kakao.connect-timeout-seconds:2}") int connectTimeoutSeconds,
            @Value("${external.kakao.read-timeout-seconds:3}") int readTimeoutSeconds
    ) {
        RestClient.Builder restClientBuilder = restClientBuilderProvider.getIfAvailable(RestClient::builder);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();

        log.info(
                "Kakao Local API timeout 설정 완료: connectTimeoutSeconds={}, readTimeoutSeconds={}",
                connectTimeoutSeconds,
                readTimeoutSeconds
        );
    }

    @PostConstruct
    void logConfiguredBaseUrl() {
        log.info("Kakao Local API base URL configured: {}", baseUrl);
    }

    public KakaoPlaceResponse searchCafe(
            double latitude,
            double longitude
    ) {
        try {
            return restClient.get()
                    .uri(UriComponentsBuilder.fromUriString(baseUrl)
                            .path("/v2/local/search/category.json")
                            .queryParam("category_group_code", "CE7")
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("radius", 2000)
                            .queryParam("sort", "distance")
                            .build()
                            .toUri())
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .body(KakaoPlaceResponse.class);
        } catch (RestClientException exception) {
            log.warn(
                    "Kakao Local 카페 검색 호출 실패 또는 timeout 발생: latitude={}, longitude={}, exceptionType={}, message={}",
                    latitude,
                    longitude,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception
            );
            throw exception;
        }
    }

    public KakaoPlaceResponse searchRestaurant(
            double latitude,
            double longitude,
            RestaurantType restaurantType
    ) {
        try {
            return restClient.get()
                    .uri(UriComponentsBuilder.fromUriString(baseUrl)
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", restaurantType.getKeyword())
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("radius", 2000)
                            .queryParam("sort", "distance")
                            .build()
                            .toUri())
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .body(KakaoPlaceResponse.class);
        } catch (RestClientException exception) {
            log.warn(
                    "Kakao Local 식당 검색 호출 실패 또는 timeout 발생: restaurantType={}, latitude={}, longitude={}, exceptionType={}, message={}",
                    restaurantType,
                    latitude,
                    longitude,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception
            );
            throw exception;
        }
    }
}