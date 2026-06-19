package come.back.gotoday.external.kakao.controller;

import come.back.gotoday.course.type.RestaurantType;
import come.back.gotoday.external.kakao.dto.KakaoPlaceResponse;
import come.back.gotoday.external.kakao.service.KakaoLocalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class KakaoTestController {

    private final KakaoLocalService kakaoLocalService;

    @GetMapping("/test/cafe")
    public KakaoPlaceResponse cafe(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        return kakaoLocalService.searchCafe(lat, lon);
    }

    @GetMapping("/test/restaurant")
    public KakaoPlaceResponse restaurant(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam RestaurantType restaurantType
    ) {
        return kakaoLocalService.searchRestaurant(lat, lon,restaurantType);
    }
}