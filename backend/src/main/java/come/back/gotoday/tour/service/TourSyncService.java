package come.back.gotoday.tour.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.category.type.CategoryType;
import come.back.gotoday.external.tour.TourApiClient;
import come.back.gotoday.external.tour.dto.TourApiItem;
import come.back.gotoday.recommend.engine.VectorEmbeddingEngine;
import come.back.gotoday.tour.entity.Tour;
import come.back.gotoday.tour.enums.TourSource;
import come.back.gotoday.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourSyncService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_NUM_OF_ROWS = 100;

    private static final String SEOUL_AREA_CODE = "1";

    private static final List<String> SEOUL_SIGUNGU_CODES = List.of(
            "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15",
            "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "25"
    );

    private final TourApiClient tourApiClient;
    private final TourRepository tourRepository;
    private final CategoryRepository categoryRepository;
    private final TourCategoryMapper tourCategoryMapper;
    private final VectorEmbeddingEngine vectorEmbeddingEngine;

    @Async("tourPlaceSyncExecutor")
    @Transactional
    public CompletableFuture<Integer> syncToursAsync(String areaCode, String sigunguCode) {
        try {
            int syncedCount = syncTours(areaCode, sigunguCode);
            return CompletableFuture.completedFuture(syncedCount);
        } catch (Exception e) {
            log.error("TourAPI 관광지 비동기 동기화 실패: areaCode={}, sigunguCode={}", areaCode, sigunguCode, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional
    public int syncTours(String areaCode, String sigunguCode) {
        List<TourApiItem> items = tourApiClient.fetchTourItems(
                areaCode,
                sigunguCode,
                DEFAULT_PAGE_NO,
                DEFAULT_NUM_OF_ROWS
        );

        int syncedCount = 0;

        for (TourApiItem item : items) {
            if (isInvalidItem(item)) {
                continue;
            }

            upsertTour(item);
            syncedCount++;
        }

        log.info("TourAPI 관광지 동기화 완료: areaCode={}, sigunguCode={}, syncedCount={}",
                areaCode, sigunguCode, syncedCount);

        return syncedCount;
    }

    @Async("tourPlaceSyncExecutor")
    @Transactional
    public CompletableFuture<Integer> syncAllSeoulToursAsync() {
        try {
            int syncedCount = syncAllSeoulTours();
            return CompletableFuture.completedFuture(syncedCount);
        } catch (Exception e) {
            log.error("TourAPI 서울 전체 관광지 비동기 동기화 실패", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional
    public int syncAllSeoulTours() {
        int totalSyncedCount = 0;

        for (String sigunguCode : SEOUL_SIGUNGU_CODES) {
            int syncedCount = syncTours(SEOUL_AREA_CODE, sigunguCode);
            totalSyncedCount += syncedCount;

            log.info("TourAPI 서울 구별 관광지 동기화 완료: sigunguCode={}, syncedCount={}",
                    sigunguCode, syncedCount);
        }

        log.info("TourAPI 서울 전체 관광지 동기화 완료: totalSyncedCount={}", totalSyncedCount);

        return totalSyncedCount;
    }

    private boolean isInvalidItem(TourApiItem item) {
        return item == null
                || isBlank(item.contentid())
                || isBlank(item.title());
    }

    private void upsertTour(TourApiItem item) {
        tourRepository.findByContentId(item.contentid())
                .ifPresentOrElse(
                        existingTour -> updateTourIfChanged(existingTour, item),
                        () -> saveNewTour(item)
                );
    }

    private void updateTourIfChanged(Tour tour, TourApiItem item) {
        Category category = findCategory(item);
        String detailCategoryName = tourCategoryMapper.mapDetailCategoryName(
                item.cat1(), item.cat2(), item.cat3());
        String area = extractArea(item.addr1());
        Double latitude = parseDouble(item.mapy());
        Double longitude = parseDouble(item.mapx());

        boolean categoryChanged = isCategoryChanged(tour, category);
        boolean tourInfoChanged = tour.isChanged(
                item.title(),
                item.addr1(),
                item.addr2(),
                item.tel(),
                tour.getHomepageUrl(),
                item.firstimage(),
                item.firstimage2(),
                tour.getOverview(),
                item.cat1(),
                item.cat2(),
                item.cat3(),
                detailCategoryName,
                area,
                latitude,
                longitude
        );
        boolean embeddingMissing = tour.getEmbeddingVector() == null
                || tour.getEmbeddingVector().length == 0;

        if (!categoryChanged && !tourInfoChanged && !embeddingMissing) {
            return;
        }

        if (categoryChanged || tourInfoChanged) {
            tour.updateInfo(
                    category,
                    item.title(),
                    item.addr1(),
                    item.addr2(),
                    item.tel(),
                    tour.getHomepageUrl(),
                    item.firstimage(),
                    item.firstimage2(),
                    tour.getOverview(),
                    item.cat1(),
                    item.cat2(),
                    item.cat3(),
                    detailCategoryName,
                    area,
                    latitude,
                    longitude
            );
        }

        tour.setEmbeddingVector(vectorEmbeddingEngine.getEmbedding(
                createEmbeddingText(
                        item.title(),
                        detailCategoryName,
                        item.addr1(),
                        tour.getOverview()
                )
        ));
    }

    private void saveNewTour(TourApiItem item) {
        String detailCategoryName = tourCategoryMapper.mapDetailCategoryName(
                item.cat1(), item.cat2(), item.cat3());
        String overview = null;
        float[] embeddingVector = vectorEmbeddingEngine.getEmbedding(
                createEmbeddingText(
                        item.title(),
                        detailCategoryName,
                        item.addr1(),
                        overview
                )
        );

        Tour tour = Tour.create(
                findCategory(item),
                item.contentid(),
                item.contenttypeid(),
                item.title(),
                item.addr1(),
                item.addr2(),
                item.tel(),
                null,
                item.firstimage(),
                item.firstimage2(),
                overview,
                item.areacode(),
                item.sigungucode(),
                item.cat1(),
                item.cat2(),
                item.cat3(),
                detailCategoryName,
                extractArea(item.addr1()),
                parseDouble(item.mapy()),
                parseDouble(item.mapx()),
                TourSource.TOUR_API,
                embeddingVector
        );

        tourRepository.save(tour);
    }

    private String createEmbeddingText(
            String title,
            String detailCategoryName,
            String address,
            String overview
    ) {
        return String.join(" ", List.of(
                nullToEmpty(title),
                nullToEmpty(detailCategoryName),
                nullToEmpty(address),
                nullToEmpty(overview)
        )).trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Category findCategory(TourApiItem item) {
        return tourCategoryMapper.mapToCategoryName(
                        item.cat1(),
                        item.cat2(),
                        item.cat3()
                )
                .flatMap(categoryName -> categoryRepository.findByNameAndType(categoryName, CategoryType.TOUR))
                .orElse(null);
    }

    private boolean isCategoryChanged(Tour tour, Category category) {
        Long currentCategoryId = tour.getCategory() == null ? null : tour.getCategory().getId();
        Long newCategoryId = category == null ? null : category.getId();

        return !Objects.equals(currentCategoryId, newCategoryId);
    }

    private String extractArea(String address) {
        if (isBlank(address)) {
            return null;
        }

        String[] parts = address.trim().split("\\s+");

        if (parts.length >= 2 && parts[0].contains("서울")) {
            return parts[1];
        }

        if (parts.length >= 2) {
            return parts[1];
        }

        return address.trim();
    }

    private Double parseDouble(String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("TourAPI 좌표 파싱 실패: value={}", value);
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}