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

    public int syncKakaoPlacesFromBasePlaces(int limit) {
        Category cafeCategory = getCategory(CAFE_CATEGORY_NAME);
        Category restaurantCategory = getCategory(RESTAURANT_CATEGORY_NAME);

        List<Place> basePlaces = placeRepository.findKakaoSyncBasePlaces(
                PageRequest.of(0, limit)
        );

        log.info("카카오 장소 DB 기준 동기화 시작: basePlaceCount={}", basePlaces.size());

        int processedCount = 0;

        for (Place basePlace : basePlaces) {
            try {
                double latitude = toDouble(basePlace.getLatitude());
                double longitude = toDouble(basePlace.getLongitude());

                processedCount += syncByCoordinate(
                        latitude,
                        longitude,
                        cafeCategory,
                        restaurantCategory
                );
            } catch (Exception e) {
                log.warn(
                        "기준 장소 주변 카카오 장소 동기화 실패: placeId={}, name={}, reason={}",
                        basePlace.getId(),
                        basePlace.getName(),
                        e.getMessage(),
                        e
                );
            }
        }

        log.info(
                "카카오 장소 DB 기준 동기화 완료: basePlaceCount={}, processedCount={}",
                basePlaces.size(),
                processedCount
        );

        return processedCount;
    }

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
        int processedCount = 0;

        KakaoPlaceResponse cafeResponse = kakaoLocalService.searchCafe(
                latitude,
                longitude
        );

        processedCount += savePlaces(cafeResponse, cafeCategory);

        for (RestaurantType restaurantType : RestaurantType.values()) {
            KakaoPlaceResponse restaurantResponse = kakaoLocalService.searchRestaurant(
                    latitude,
                    longitude,
                    restaurantType
            );

            processedCount += savePlaces(restaurantResponse, restaurantCategory);
        }

        return processedCount;
    }

    private int savePlaces(KakaoPlaceResponse response, Category category) {
        if (response == null || response.documents() == null) {
            return 0;
        }

        int processedCount = 0;

        for (var doc : response.documents()) {
            if (doc.y() == null || doc.x() == null) {
                continue;
            }

            try {
                placeService.getOrCreatePlace(doc, category);
                processedCount++;
            } catch (Exception e) {
                log.warn(
                        "카카오 장소 저장 실패: name={}, address={}, reason={}",
                        doc.placeName(),
                        doc.addressName(),
                        e.getMessage(),
                        e
                );
            }
        }

        return processedCount;
    }

    private Category getCategory(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new IllegalStateException(categoryName + " 카테고리가 존재하지 않습니다."));
    }

    private double toDouble(BigDecimal value) {
        return value.doubleValue();
    }
}