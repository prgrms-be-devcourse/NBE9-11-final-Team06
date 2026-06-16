package come.back.gotoday.external.naver;

import come.back.gotoday.external.naver.config.NaverSearchProperties;
import come.back.gotoday.external.naver.dto.NaverLocalSearchResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

        try {
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

            if (response == null) {
                log.warn("네이버 지역 검색 API 응답이 비어 있습니다. query={}", query);
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
            }

            log.info("네이버 지역 검색 API 호출 완료: query={}", query);
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.error("네이버 지역 검색 API 호출 실패: query={}, display={}, start={}", query, display, start, exception);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }
}
