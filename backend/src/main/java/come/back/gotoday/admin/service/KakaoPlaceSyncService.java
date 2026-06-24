package come.back.gotoday.admin.service;

import come.back.gotoday.admin.dto.request.AdminKakaoPlaceSyncRequest;
import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.course.type.RestaurantType;
import come.back.gotoday.external.kakao.dto.KakaoPlaceResponse;
import come.back.gotoday.external.kakao.service.KakaoLocalService;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import come.back.gotoday.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPlaceSyncService {

    private static final String CAFE_CATEGORY_NAME = "카페";
    private static final String RESTAURANT_CATEGORY_NAME = "맛집";

    private final KakaoLocalService kakaoLocalService;
    private final PlaceService placeService;
    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public int syncKakaoPlacesFromBasePlaces(int limit) {
        Category cafeCategory = getCategory(CAFE_CATEGORY_NAME);
        Category restaurantCategory = getCategory(RESTAURANT_CATEGORY_NAME);

        List<Place> basePlaces = placeRepository.findKakaoSyncBasePlaces(
                PageRequest.of(0, limit)
        );

        log.info("카카오 장소 DB 기준 동기화 시작: basePlaceCount={}", basePlaces.size());

        int processedCount = 0;

        for (Place basePlace : basePlaces) {
            double latitude = toDouble(basePlace.getLatitude());
            double longitude = toDouble(basePlace.getLongitude());

            processedCount += syncByCoordinate(
                    latitude,
                    longitude,
                    cafeCategory,
                    restaurantCategory
            );
        }

        log.info(
                "카카오 장소 DB 기준 동기화 완료: basePlaceCount={}, processedCount={}",
                basePlaces.size(),
                processedCount
        );

        return processedCount;
    }

    @Transactional
    public int syncKakaoPlacesNearby(AdminKakaoPlaceSyncRequest request) {
        Category cafeCategory = getCategory(CAFE_CATEGORY_NAME);
        Category restaurantCategory = getCategory(RESTAURANT_CATEGORY_NAME);

        return syncByCoordinate(
                request.latitude(),
                request.longitude(),
                cafeCategory,
                restaurantCategory
        );
    }

    private int syncByCoordinate(
            double latitude,
            double longitude,
            Category cafeCategory,
            Category restaurantCategory
    ) {
        KakaoPlaceResponse cafeResponse = kakaoLocalService.searchCafe(
                latitude,
                longitude
        );

        int cafeCount = savePlaces(cafeResponse, cafeCategory);

        int restaurantCount = 0;

        for (RestaurantType restaurantType : RestaurantType.values()) {
            KakaoPlaceResponse restaurantResponse = kakaoLocalService.searchRestaurant(
                    latitude,
                    longitude,
                    restaurantType
            );

            restaurantCount += savePlaces(restaurantResponse, restaurantCategory);
        }

        return cafeCount + restaurantCount;
    }

    private int savePlaces(KakaoPlaceResponse response, Category category) {
        if (response == null || response.documents() == null) {
            return 0;
        }

        return response.documents()
                .stream()
                .filter(doc -> doc.y() != null && doc.x() != null)
                .map(doc -> placeService.getOrCreatePlace(doc, category))
                .toList()
                .size();
    }

    private Category getCategory(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new IllegalStateException(categoryName + " 카테고리가 존재하지 않습니다."));
    }

    private double toDouble(BigDecimal value) {
        return value.doubleValue();
    }
}