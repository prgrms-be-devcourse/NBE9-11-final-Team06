package come.back.gotoday.external.naver;

import come.back.gotoday.external.naver.config.NaverSearchProperties;
import come.back.gotoday.external.naver.dto.NaverLocalSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverLocalSearchClient {

    private static final String NAVER_CLIENT_ID_HEADER = "X-Naver-Client-Id";
    private static final String NAVER_CLIENT_SECRET_HEADER = "X-Naver-Client-Secret";
    private static final String LOCAL_SEARCH_PATH = "/v1/search/local.json";

    private final RestClient naverSearchRestClient;
    private final NaverSearchProperties properties;

    public NaverLocalSearchResponse search(String query, int display, int start) {
        log.info("네이버 지역 검색 API 호출 시작: query={}, display={}, start={}", query, display, start);

        NaverLocalSearchResponse response = naverSearchRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(LOCAL_SEARCH_PATH)
                        .queryParam("query", query)
                        .queryParam("display", display)
                        .queryParam("start", start)
                        .queryParam("sort", "random")
                        .build())
                .header(NAVER_CLIENT_ID_HEADER, properties.clientId())
                .header(NAVER_CLIENT_SECRET_HEADER, properties.clientSecret())
                .retrieve()
                .body(NaverLocalSearchResponse.class);

        log.info("네이버 지역 검색 API 호출 완료: query={}", query);
        return response;
    }
}
