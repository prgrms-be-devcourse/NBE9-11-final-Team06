package come.back.gotoday.external.tour;

import come.back.gotoday.external.tour.dto.TourApiResponse;
import come.back.gotoday.external.tour.dto.TourApiResponse.TourPlaceItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class TourApiClient {

    private static final String TOUR_PLACE_CONTENT_TYPE_ID = "12";

    private final RestClient restClient;
    private final TourApiProperties properties;

    public TourApiClient(TourApiProperties properties) {
        this.restClient = RestClient.builder().build();
        this.properties = properties;
    }

    public List<TourPlaceItem> fetchTourPlaces(String areaCode, int pageNo, int numOfRows) {
        URI uri = createAreaBasedListUri(areaCode, pageNo, numOfRows);

        TourApiResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(TourApiResponse.class);

        return extractItems(response);
    }

    private URI createAreaBasedListUri(String areaCode, int pageNo, int numOfRows) {
        String url = properties.getBaseUrl()
                + "/areaBasedList2"
                + "?serviceKey=" + properties.getApiKey()
                + "&MobileOS=" + properties.getMobileOs()
                + "&MobileApp=" + properties.getMobileApp()
                + "&_type=json"
                + "&contentTypeId=" + TOUR_PLACE_CONTENT_TYPE_ID
                + "&areaCode=" + areaCode
                + "&pageNo=" + pageNo
                + "&numOfRows=" + numOfRows
                + "&arrange=A";

        return URI.create(url);
    }

    private List<TourPlaceItem> extractItems(TourApiResponse response) {
        if (response == null
                || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody().getItems().getItem() == null) {
            return List.of();
        }

        return response.getResponse()
                .getBody()
                .getItems()
                .getItem();
    }
}