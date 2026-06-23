package come.back.gotoday.external.tour.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.external.tour.TourApiClient;
import come.back.gotoday.external.tour.dto.TourApiResponse.TourPlaceItem;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourPlaceSyncService {

    private static final String TOUR_PLACE_CATEGORY_NAME = "관광지";
    private static final int DEFAULT_NUM_OF_ROWS = 100;

    private final TourApiClient tourApiClient;
    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public int syncTourPlaces(String areaCode) {
        Category tourCategory = getTourCategory();

        int pageNo = 1;
        int savedCount = 0;

        while (true) {
            List<TourPlaceItem> items = tourApiClient.fetchTourPlaces(
                    areaCode,
                    pageNo,
                    DEFAULT_NUM_OF_ROWS
            );

            if (items.isEmpty()) {
                break;
            }

            for (TourPlaceItem item : items) {
                if (!isValidTourPlace(item)) {
                    continue;
                }

                saveOrUpdateTourPlace(tourCategory, item);
                savedCount++;
            }

            if (items.size() < DEFAULT_NUM_OF_ROWS) {
                break;
            }

            pageNo++;
        }

        log.info("관광공사 관광지 동기화 완료 - areaCode={}, savedCount={}", areaCode, savedCount);

        return savedCount;
    }

    private Category getTourCategory() {
        return categoryRepository.findByName(TOUR_PLACE_CATEGORY_NAME)
                .orElseThrow(() -> new IllegalStateException("관광지 카테고리가 존재하지 않습니다."));
    }

    private boolean isValidTourPlace(TourPlaceItem item) {
        return item.getContentId() != null
                && item.getTitle() != null
                && item.getLatitude() != null
                && item.getLongitude() != null;
    }

    private void saveOrUpdateTourPlace(Category category, TourPlaceItem item) {
        placeRepository.findBySourceAndExternalId(Place.TOUR_API_SOURCE, item.getContentId())
                .ifPresentOrElse(
                        place -> updateTourPlace(place, category, item),
                        () -> createTourPlace(category, item)
                );
    }

    private void createTourPlace(Category category, TourPlaceItem item) {
        Place place = Place.createTourPlace(
                category,
                item.getTitle(),
                item.getAddr1(),
                item.getAddr2(),
                item.getLatitude(),
                item.getLongitude(),
                item.getTel(),
                null,
                createDescription(item),
                item.getContentId()
        );

        placeRepository.save(place);
    }

    private void updateTourPlace(Place place, Category category, TourPlaceItem item) {
        place.updateTourInfo(
                category,
                item.getTitle(),
                item.getAddr1(),
                item.getAddr2(),
                item.getLatitude(),
                item.getLongitude(),
                item.getTel(),
                null,
                createDescription(item)
        );
    }

    private String createDescription(TourPlaceItem item) {
        return "한국관광공사 관광지 데이터";
    }
}