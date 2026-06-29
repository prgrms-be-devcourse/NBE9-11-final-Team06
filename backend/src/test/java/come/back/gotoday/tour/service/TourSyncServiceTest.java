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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourSyncServiceTest {

    @Mock
    private TourApiClient tourApiClient;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TourCategoryMapper tourCategoryMapper;

    @Mock
    private VectorEmbeddingEngine vectorEmbeddingEngine;

    @InjectMocks
    private TourSyncService tourSyncService;

    @Test
    @DisplayName("관광지 동기화에 성공하면 Tour를 저장하고 동기화 개수를 반환한다")
    void syncTours_success() {
        // given
        String areaCode = "1";
        String sigunguCode = "23";

        TourApiItem item = createTourApiItem(
                "100",
                "12",
                "홍대 걷고싶은거리",
                "서울 마포구 어울마당로 123",
                "상세주소",
                "02-123-4567",
                "https://example.com/image.jpg",
                "https://example.com/thumb.jpg",
                areaCode,
                sigunguCode,
                "A02",
                "A0203",
                "A02030600",
                "126.923",
                "37.556"
        );

        Category category = createCategory(1L, "관광지", CategoryType.TOUR);

        when(tourApiClient.fetchTourItems(areaCode, sigunguCode, 1, 100))
                .thenReturn(List.of(item));
        when(tourRepository.findByContentId("100"))
                .thenReturn(Optional.empty());
        when(tourCategoryMapper.mapDetailCategoryName("A02", "A0203", "A02030600"))
                .thenReturn("문화관광지");
        when(tourCategoryMapper.mapToCategoryName("A02", "A0203", "A02030600"))
                .thenReturn(Optional.of("관광지"));
        when(categoryRepository.findByNameAndType("관광지", CategoryType.TOUR))
                .thenReturn(Optional.of(category));
        when(vectorEmbeddingEngine.getEmbedding(anyString()))
                .thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        // when
        int syncedCount = tourSyncService.syncTours(areaCode, sigunguCode);

        // then
        assertThat(syncedCount).isEqualTo(1);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());

        Tour savedTour = tourCaptor.getValue();

        assertThat(savedTour.getCategory()).isEqualTo(category);
        assertThat(savedTour.getContentId()).isEqualTo("100");
        assertThat(savedTour.getTitle()).isEqualTo("홍대 걷고싶은거리");
        assertThat(savedTour.getArea()).isEqualTo("마포구");
        assertThat(savedTour.getLatitude()).isEqualTo(37.556);
        assertThat(savedTour.getLongitude()).isEqualTo(126.923);
        assertThat(savedTour.getEmbeddingVector()).containsExactly(0.1f, 0.2f, 0.3f);

        verify(tourApiClient).fetchTourItems(areaCode, sigunguCode, 1, 100);
        verify(tourRepository).findByContentId("100");
        verify(vectorEmbeddingEngine).getEmbedding(anyString());
    }

    @Test
    @DisplayName("contentId 또는 title이 없는 관광지는 동기화에서 제외한다")
    void syncTours_skipInvalidItems() {
        // given
        String areaCode = "1";
        String sigunguCode = "23";

        TourApiItem nullContentIdItem = createTourApiItem(
                null,
                "12",
                "제목 있음",
                "서울 마포구 어울마당로 123",
                null,
                null,
                null,
                null,
                areaCode,
                sigunguCode,
                "A02",
                "A0203",
                "A02030600",
                "126.923",
                "37.556"
        );

        TourApiItem blankTitleItem = createTourApiItem(
                "200",
                "12",
                " ",
                "서울 마포구 어울마당로 123",
                null,
                null,
                null,
                null,
                areaCode,
                sigunguCode,
                "A02",
                "A0203",
                "A02030600",
                "126.923",
                "37.556"
        );

        when(tourApiClient.fetchTourItems(areaCode, sigunguCode, 1, 100))
                .thenReturn(Arrays.asList(null, nullContentIdItem, blankTitleItem));

        // when
        int syncedCount = tourSyncService.syncTours(areaCode, sigunguCode);

        // then
        assertThat(syncedCount).isZero();

        verify(tourRepository, never()).findByContentId(anyString());
        verify(tourRepository, never()).save(any(Tour.class));
        verifyNoInteractions(categoryRepository);
        verifyNoInteractions(vectorEmbeddingEngine);
    }

    @Test
    @DisplayName("Tour API 응답이 비어 있으면 동기화 개수는 0이다")
    void syncTours_emptyItems() {
        // given
        String areaCode = "1";
        String sigunguCode = "23";

        when(tourApiClient.fetchTourItems(areaCode, sigunguCode, 1, 100))
                .thenReturn(List.of());

        // when
        int syncedCount = tourSyncService.syncTours(areaCode, sigunguCode);

        // then
        assertThat(syncedCount).isZero();

        verify(tourApiClient).fetchTourItems(areaCode, sigunguCode, 1, 100);
        verifyNoInteractions(tourRepository);
        verifyNoInteractions(categoryRepository);
        verifyNoInteractions(vectorEmbeddingEngine);
    }

    @Test
    @DisplayName("이미 저장된 관광지가 있고 변경 사항이 없으면 저장하지 않는다")
    void syncTours_existingTour_noChange() {
        // given
        String areaCode = "1";
        String sigunguCode = "23";

        TourApiItem item = createTourApiItem(
                "100",
                "12",
                "홍대 걷고싶은거리",
                "서울 마포구 어울마당로 123",
                "상세주소",
                "02-123-4567",
                "https://example.com/image.jpg",
                "https://example.com/thumb.jpg",
                areaCode,
                sigunguCode,
                "A02",
                "A0203",
                "A02030600",
                "126.923",
                "37.556"
        );

        Category category = createCategory(1L, "관광지", CategoryType.TOUR);

        Tour existingTour = Tour.create(
                category,
                "100",
                "12",
                "홍대 걷고싶은거리",
                "서울 마포구 어울마당로 123",
                "상세주소",
                "02-123-4567",
                null,
                "https://example.com/image.jpg",
                "https://example.com/thumb.jpg",
                null,
                areaCode,
                sigunguCode,
                "A02",
                "A0203",
                "A02030600",
                "문화관광지",
                "마포구",
                37.556,
                126.923,
                TourSource.TOUR_API,
                new float[]{0.1f, 0.2f}
        );

        when(tourApiClient.fetchTourItems(areaCode, sigunguCode, 1, 100))
                .thenReturn(List.of(item));
        when(tourRepository.findByContentId("100"))
                .thenReturn(Optional.of(existingTour));
        when(tourCategoryMapper.mapDetailCategoryName("A02", "A0203", "A02030600"))
                .thenReturn("문화관광지");
        when(tourCategoryMapper.mapToCategoryName("A02", "A0203", "A02030600"))
                .thenReturn(Optional.of("관광지"));
        when(categoryRepository.findByNameAndType("관광지", CategoryType.TOUR))
                .thenReturn(Optional.of(category));

        // when
        int syncedCount = tourSyncService.syncTours(areaCode, sigunguCode);

        // then
        assertThat(syncedCount).isEqualTo(1);

        verify(tourRepository, never()).save(any(Tour.class));
        verify(vectorEmbeddingEngine, never()).getEmbedding(anyString());
    }

    @Test
    @DisplayName("이미 저장된 관광지의 임베딩이 없으면 임베딩을 생성한다")
    void syncTours_existingTour_embeddingMissing() {
        // given
        String areaCode = "1";
        String sigunguCode = "23";

        TourApiItem item = createTourApiItem(
                "100",
                "12",
                "홍대 걷고싶은거리",
                "서울 마포구 어울마당로 123",
                "상세주소",
                "02-123-4567",
                "https://example.com/image.jpg",
                "https://example.com/thumb.jpg",
                areaCode,
                sigunguCode,
                "A02",
                "A0203",
                "A02030600",
                "126.923",
                "37.556"
        );

        Category category = createCategory(1L, "관광지", CategoryType.TOUR);

        Tour existingTour = Tour.create(
                category,
                "100",
                "12",
                "홍대 걷고싶은거리",
                "서울 마포구 어울마당로 123",
                "상세주소",
                "02-123-4567",
                null,
                "https://example.com/image.jpg",
                "https://example.com/thumb.jpg",
                null,
                areaCode,
                sigunguCode,
                "A02",
                "A0203",
                "A02030600",
                "문화관광지",
                "마포구",
                37.556,
                126.923,
                TourSource.TOUR_API,
                null
        );

        when(tourApiClient.fetchTourItems(areaCode, sigunguCode, 1, 100))
                .thenReturn(List.of(item));
        when(tourRepository.findByContentId("100"))
                .thenReturn(Optional.of(existingTour));
        when(tourCategoryMapper.mapDetailCategoryName("A02", "A0203", "A02030600"))
                .thenReturn("문화관광지");
        when(tourCategoryMapper.mapToCategoryName("A02", "A0203", "A02030600"))
                .thenReturn(Optional.of("관광지"));
        when(categoryRepository.findByNameAndType("관광지", CategoryType.TOUR))
                .thenReturn(Optional.of(category));
        when(vectorEmbeddingEngine.getEmbedding(anyString()))
                .thenReturn(new float[]{0.5f, 0.6f});

        // when
        int syncedCount = tourSyncService.syncTours(areaCode, sigunguCode);

        // then
        assertThat(syncedCount).isEqualTo(1);
        assertThat(existingTour.getEmbeddingVector()).containsExactly(0.5f, 0.6f);

        verify(tourRepository, never()).save(any(Tour.class));
        verify(vectorEmbeddingEngine).getEmbedding(anyString());
    }

    @Test
    @DisplayName("서울 전체 관광지 동기화는 25개 시군구 코드를 순회한다")
    void syncAllSeoulTours_success() {
        // given
        when(tourApiClient.fetchTourItems(eq("1"), anyString(), eq(1), eq(100)))
                .thenReturn(List.of());

        // when
        int syncedCount = tourSyncService.syncAllSeoulTours();

        // then
        assertThat(syncedCount).isZero();

        verify(tourApiClient, times(25))
                .fetchTourItems(eq("1"), anyString(), eq(1), eq(100));
    }

    @Test
    @DisplayName("비동기 관광지 동기화에 성공하면 완료된 Future를 반환한다")
    void syncToursAsync_success() {
        // given
        String areaCode = "1";
        String sigunguCode = "23";

        when(tourApiClient.fetchTourItems(areaCode, sigunguCode, 1, 100))
                .thenReturn(List.of());

        // when
        CompletableFuture<Integer> future =
                tourSyncService.syncToursAsync(areaCode, sigunguCode);

        // then
        assertThat(future.join()).isZero();
    }

    @Test
    @DisplayName("비동기 관광지 동기화 중 예외가 발생하면 실패한 Future를 반환한다")
    void syncToursAsync_fail() {
        // given
        String areaCode = "1";
        String sigunguCode = "23";

        when(tourApiClient.fetchTourItems(areaCode, sigunguCode, 1, 100))
                .thenThrow(new RuntimeException("Tour API error"));

        // when
        CompletableFuture<Integer> future =
                tourSyncService.syncToursAsync(areaCode, sigunguCode);

        // then
        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    private Category createCategory(Long id, String name, CategoryType type) {
        Category category = Category.create(name, type);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private TourApiItem createTourApiItem(
            String contentId,
            String contentTypeId,
            String title,
            String addr1,
            String addr2,
            String tel,
            String firstImage,
            String firstImage2,
            String areaCode,
            String sigunguCode,
            String cat1,
            String cat2,
            String cat3,
            String mapX,
            String mapY
    ) {
        Map<String, Object> values = new HashMap<>();

        values.put("contentid", contentId);
        values.put("contenttypeid", contentTypeId);
        values.put("title", title);
        values.put("addr1", addr1);
        values.put("addr2", addr2);
        values.put("tel", tel);
        values.put("firstimage", firstImage);
        values.put("firstimage2", firstImage2);
        values.put("areacode", areaCode);
        values.put("sigungucode", sigunguCode);
        values.put("cat1", cat1);
        values.put("cat2", cat2);
        values.put("cat3", cat3);
        values.put("mapx", mapX);
        values.put("mapy", mapY);

        return createRecord(TourApiItem.class, values);
    }

    private static <T> T createRecord(Class<T> recordType, Map<String, Object> values) {
        try {
            RecordComponent[] components = recordType.getRecordComponents();

            Class<?>[] parameterTypes = new Class<?>[components.length];
            Object[] args = new Object[components.length];

            for (int i = 0; i < components.length; i++) {
                parameterTypes[i] = components[i].getType();
                args[i] = values.get(components[i].getName());
            }

            Constructor<T> constructor = recordType.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);

            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "테스트용 record 생성에 실패했습니다: " + recordType.getSimpleName(),
                    e
            );
        }
    }
}