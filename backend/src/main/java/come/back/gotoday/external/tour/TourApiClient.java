package come.back.gotoday.external.tour;

import come.back.gotoday.external.tour.dto.TourApiItem;
import come.back.gotoday.external.tour.dto.TourApiResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class TourApiClient {

    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "GoToday";
    private static final String RESPONSE_TYPE = "json";
    private static final String TOUR_CONTENT_TYPE_ID = "12";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${external.tour.api-key:}")
    private String serviceKey;

    @Value("${external.tour.base-url:https://apis.data.go.kr/B551011/KorService2}")
    private String baseUrl;

    public List<TourApiItem> fetchTourItems(
            String areaCode,
            String sigunguCode,
            int pageNo,
            int numOfRows
    ) {
        URI uri = buildAreaBasedListUri(areaCode, sigunguCode, pageNo, numOfRows);

        log.info("TourAPI 관광지 목록 조회 시작: areaCode={}, sigunguCode={}, pageNo={}, numOfRows={}, keyLoaded={}, encodedKey={}",
                areaCode,
                sigunguCode,
                pageNo,
                numOfRows,
                isServiceKeyLoaded(),
                isEncodedServiceKey()
        );

        try {
            TourApiResponseWrapper response = restTemplate.getForObject(
                    uri,
                    TourApiResponseWrapper.class
            );

            if (response == null) {
                log.warn("TourAPI 관광지 목록 조회 응답이 비어있습니다.");
                return List.of();
            }

            List<TourApiItem> items = response.getItemsOrEmpty();

            log.info("TourAPI 관광지 목록 조회 성공: count={}, totalCount={}",
                    items.size(),
                    response.getTotalCountOrZero()
            );

            return items;
        } catch (HttpClientErrorException e) {
            log.warn("TourAPI 관광지 목록 조회 실패: status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            throw e;
        }
    }

    private URI buildAreaBasedListUri(
            String areaCode,
            String sigunguCode,
            int pageNo,
            int numOfRows
    ) {
        if (!isServiceKeyLoaded()) {
            throw new IllegalStateException("TourAPI 인증키가 설정되지 않았습니다.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(getAreaBasedListUrl())
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", RESPONSE_TYPE)
                .queryParam("contentTypeId", TOUR_CONTENT_TYPE_ID)
                .queryParam("arrange", "O")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows);

        if (areaCode != null && !areaCode.isBlank()) {
            builder.queryParam("areaCode", areaCode);
        }

        if (sigunguCode != null && !sigunguCode.isBlank()) {
            builder.queryParam("sigunguCode", sigunguCode);
        }

        if (isEncodedServiceKey()) {
            return builder.build(true).toUri();
        }

        return builder.encode(StandardCharsets.UTF_8).build().toUri();
    }

    private String getAreaBasedListUrl() {
        if (baseUrl.endsWith("/")) {
            return baseUrl + "areaBasedList2";
        }

        return baseUrl + "/areaBasedList2";
    }

    private boolean isServiceKeyLoaded() {
        return serviceKey != null && !serviceKey.isBlank();
    }

    private boolean isEncodedServiceKey() {
        return serviceKey != null && serviceKey.contains("%");
    }
}