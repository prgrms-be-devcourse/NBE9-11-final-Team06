package come.back.gotoday.external.kakao.service;

import come.back.gotoday.external.kakao.dto.KakaoPlaceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class KakaoLocalService {

    private final RestClient restClient;

    @Value("${external.kakao.api-key}")
    private String apiKey;

    public KakaoPlaceResponse searchCafe(
            double latitude,
            double longitude
    ) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("dapi.kakao.com")
                        .path("/v2/local/search/category.json")
                        .queryParam("category_group_code", "CE7")
                        .queryParam("x", longitude)
                        .queryParam("y", latitude)
                        .queryParam("radius", 2000)
                        .queryParam("sort", "distance")
                        .build())
                .header("Authorization", "KakaoAK " + apiKey)
                .retrieve()
                .body(KakaoPlaceResponse.class);
    }

    public KakaoPlaceResponse searchRestaurant(
            double latitude,
            double longitude
    ) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("dapi.kakao.com")
                        .path("/v2/local/search/category.json")
                        .queryParam("category_group_code", "FD6")
                        .queryParam("x", longitude)
                        .queryParam("y", latitude)
                        .queryParam("radius", 2000)
                        .queryParam("sort", "distance")
                        .build())
                .header("Authorization", "KakaoAK " + apiKey)
                .retrieve()
                .body(KakaoPlaceResponse.class);
    }
}