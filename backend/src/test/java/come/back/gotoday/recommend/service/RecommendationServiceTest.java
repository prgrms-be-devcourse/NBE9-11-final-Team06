package come.back.gotoday.recommend.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.PreferenceEventCategoryMappingRepository;
import come.back.gotoday.category.repository.PreferenceTourCategoryMappingRepository;
import come.back.gotoday.crowd.service.CrowdScoreCalculator;
import come.back.gotoday.crowd.service.NearestCrowdAreaService;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.event.service.EventScheduleMatcher;
import come.back.gotoday.external.ai.service.AiRecommendationReasonService;
import come.back.gotoday.recommend.engine.SearchUtils;
import come.back.gotoday.recommend.engine.VectorEmbeddingEngine;
import come.back.gotoday.recommend.dto.RecommendationCourseCreateRequest;
import come.back.gotoday.tour.repository.TourRepository;
import come.back.gotoday.tour.entity.Tour;
import come.back.gotoday.preference.entity.UserPreference;
import come.back.gotoday.preference.repository.UserPreferenceCategoryRepository;
import come.back.gotoday.preference.repository.UserPreferenceRepository;
import come.back.gotoday.weather.service.EventIndoorOutdoorPolicy;
import come.back.gotoday.weather.service.WeatherConditionClassifier;
import come.back.gotoday.weather.service.WeatherForecastService;
import come.back.gotoday.weather.service.WeatherScoreCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("추천 서비스 단위 테스트")
class RecommendationServiceTest {


    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private UserPreferenceCategoryRepository userPreferenceCategoryRepository;

    @Mock
    private PreferenceEventCategoryMappingRepository preferenceEventCategoryMappingRepository;

    @Mock
    private PreferenceTourCategoryMappingRepository preferenceTourCategoryMappingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private NearestCrowdAreaService nearestCrowdAreaService;

    @Mock
    private CrowdScoreCalculator crowdScoreCalculator;

    @Mock
    private EventScheduleMatcher eventScheduleMatcher;

    @Mock
    private SearchUtils searchUtils;

    @Mock
    private VectorEmbeddingEngine vectorEngine;

    @Mock
    private AiRecommendationReasonService aiRecommendationReasonService;


    @Mock
    private WeatherForecastService weatherForecastService;

    @Mock
    private WeatherConditionClassifier weatherConditionClassifier;

    @Mock
    private WeatherScoreCalculator weatherScoreCalculator;

    @Mock
    private EventIndoorOutdoorPolicy eventIndoorOutdoorPolicy;

    @InjectMocks
    private RecommendationService recommendationService;
    @Test
    @DisplayName("저장된 PREFERENCE 카테고리를 매핑된 EVENT 카테고리 ID로 변환해 조회한다")
    void getRecommendedEventIdsUsesMappedEventCategoryIds() {
        Long memberId = 1L;
        Long preferenceId = 10L;
        LocalDate startDate = LocalDate.of(2026, 6, 20);
        LocalDate endDate = LocalDate.of(2026, 6, 21);

        UserPreference preference = org.mockito.Mockito.mock(UserPreference.class);
        given(preference.getId()).willReturn(preferenceId);
        given(preference.getPreferredArea()).willReturn("종로구");
        given(preference.getAvoidCrowded()).willReturn(false);

        given(userPreferenceRepository.findByMemberId(memberId))
                .willReturn(Optional.of(preference));
        given(userPreferenceCategoryRepository.findCategoryIdsByPreferenceId(preferenceId))
                .willReturn(List.of(101L));
        given(preferenceEventCategoryMappingRepository
                .findEventCategoryIdsByPreferenceCategoryIds(List.of(101L)))
                .willReturn(List.of(201L, 202L));
        given(eventRepository.findRecommendedEventsWithCategoryIds(
                "종로구",
                startDate,
                endDate,
                Set.of(201L, 202L)
        )).willReturn(List.of());
        given(eventRepository.findRecommendedEvents("종로구", startDate, endDate))
                .willReturn(List.of());
        given(eventRepository.findRecommendedEvents(
                "종로구",
                startDate.minusDays(7),
                endDate.plusDays(7)
        )).willReturn(List.of());
        given(eventRepository.findAllEventsByDate(startDate, endDate))
                .willReturn(List.of());

        List<Long> result = recommendationService.getRecommendedEventIds(
                memberId,
                "전시 추천",
                startDate,
                endDate,
                3
        );

        assertThat(result).isEmpty();
        verify(preferenceEventCategoryMappingRepository)
                .findEventCategoryIdsByPreferenceCategoryIds(List.of(101L));
        verify(eventRepository).findRecommendedEventsWithCategoryIds(
                "종로구",
                startDate,
                endDate,
                Set.of(201L, 202L)
        );
    }

    @Test
    @DisplayName("카테고리 매핑 결과가 없으면 카테고리 조건 없이 지역과 기간으로 재조회한다")
    void getRecommendedEventIdsFallsBackWhenMappingDoesNotExist() {
        Long memberId = 1L;
        Long preferenceId = 10L;
        LocalDate startDate = LocalDate.of(2026, 6, 20);
        LocalDate endDate = LocalDate.of(2026, 6, 21);

        UserPreference preference = org.mockito.Mockito.mock(UserPreference.class);
        given(preference.getId()).willReturn(preferenceId);
        given(preference.getPreferredArea()).willReturn("마포구");
        given(preference.getAvoidCrowded()).willReturn(false);

        given(userPreferenceRepository.findByMemberId(memberId))
                .willReturn(Optional.of(preference));
        given(userPreferenceCategoryRepository.findCategoryIdsByPreferenceId(preferenceId))
                .willReturn(List.of(301L));
        given(preferenceEventCategoryMappingRepository
                .findEventCategoryIdsByPreferenceCategoryIds(List.of(301L)))
                .willReturn(List.of());
        given(eventRepository.findRecommendedEvents("마포구", startDate, endDate))
                .willReturn(List.of());
        given(eventRepository.findRecommendedEvents(
                "마포구",
                startDate.minusDays(7),
                endDate.plusDays(7)
        )).willReturn(List.of());
        given(eventRepository.findAllEventsByDate(startDate, endDate))
                .willReturn(List.of());

        List<Long> result = recommendationService.getRecommendedEventIds(
                memberId,
                "산책 추천",
                startDate,
                endDate,
                3
        );

        assertThat(result).isEmpty();
        verify(eventRepository, never()).findRecommendedEventsWithCategoryIds(
                anyString(),
                any(LocalDate.class),
                any(LocalDate.class),
                any()
        );
        verify(eventRepository).findRecommendedEvents(
                "마포구",
                startDate,
                endDate
        );
    }

    @Test
    @DisplayName("추천 이유는 매핑된 EVENT 카테고리 ID를 기준으로 생성한다")
    void createRecommendationReasonUsesMappedEventCategoryId() {
        Category eventCategory = org.mockito.Mockito.mock(Category.class);
        given(eventCategory.getId()).willReturn(201L);

        Event event = org.mockito.Mockito.mock(Event.class);
        given(event.getArea()).willReturn("종로구");
        given(event.getCategory()).willReturn(eventCategory);

        String reason = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "createRecommendationReason",
                event,
                "종로구",
                Set.of(201L),
                Set.of("전시")
        );

        assertThat(reason).isEqualTo("선택한 지역과 카테고리에 모두 부합하는 행사입니다.");
    }

    @Test
    @DisplayName("성수 입력 지역은 행사 조회 기준 성동구로 정규화한다")
    void normalizeRecommendationAreaConvertsSeongsuToSeongdongGu() {
        String normalizedArea = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "normalizeRecommendationArea",
                "성수"
        );

        assertThat(normalizedArea).isEqualTo("성동구");
    }

    @Test
    @DisplayName("직접 선택한 EVENT 카테고리명도 추천 이유에 반영한다")
    void createRecommendationReasonUsesDirectlySelectedEventCategoryName() {
        Category eventCategory = org.mockito.Mockito.mock(Category.class);
        given(eventCategory.getId()).willReturn(201L);
        given(eventCategory.getName()).willReturn("전시");

        Event event = org.mockito.Mockito.mock(Event.class);
        given(event.getArea()).willReturn("성동구");
        given(event.getCategory()).willReturn(eventCategory);

        String reason = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "createRecommendationReason",
                event,
                "성동구",
                Set.of(),
                Set.of("전시")
        );

        assertThat(reason).isEqualTo("선택한 지역과 카테고리에 모두 부합하는 행사입니다.");
    }

    @Test
    @DisplayName("프론트 선택 지역·카테고리·동행 유형이 추천 검색어에 반영된다")
    void createQueryTextUsesFrontendSelectedConditions() {
        String queryText = recommendationService.createQueryText(
                "강남구",
                "전시, 카페",
                "커플"
        );

        assertThat(queryText)
                .contains("강남구")
                .contains("전시")
                .contains("카페")
                .contains("커플");
    }

    @Test
    @DisplayName("추천 조건이 null이면 기본 지역과 카테고리를 사용한다")
    void createQueryTextUsesDefaultsWhenConditionsAreNull() {
        String queryText = recommendationService.createQueryText(null, null, null);

        assertThat(queryText)
                .contains("서울")
                .contains("전체");
    }

    @Test
    @DisplayName("추천 조건이 공백이면 기본 지역과 카테고리를 사용한다")
    void createQueryTextUsesDefaultsWhenConditionsAreBlank() {
        String queryText = recommendationService.createQueryText("   ", "   ", "   ");

        assertThat(queryText)
                .contains("서울")
                .contains("전체");
    }

    @Test
    @DisplayName("일부 조건만 null이면 해당 조건에만 기본값을 적용한다")
    void createQueryTextUsesDefaultOnlyForMissingCondition() {
        String queryText = recommendationService.createQueryText(
                null,
                "전시, 카페",
                "친구"
        );

        assertThat(queryText)
                .contains("서울")
                .contains("전시")
                .contains("카페")
                .contains("친구");
    }

    @Test
    @DisplayName("null 조건으로 생성한 검색어에 null 문자열이 포함되지 않는다")
    void createQueryTextDoesNotContainLiteralNull() {
        String queryText = recommendationService.createQueryText(null, null, null);

        assertThat(queryText)
                .isNotBlank()
                .doesNotContain("null");
    }

    @Test
    @DisplayName("동일한 조건으로 검색어를 생성하면 항상 같은 결과를 반환한다")
    void createQueryTextIsDeterministic() {
        String firstQueryText = recommendationService.createQueryText(
                "종로구",
                "전시, 공연",
                "가족"
        );
        String secondQueryText = recommendationService.createQueryText(
                "종로구",
                "전시, 공연",
                "가족"
        );

        assertThat(secondQueryText).isEqualTo(firstQueryText);
    }

    @Test
    @DisplayName("저장된 선호 정보가 없으면 행사 추천 조회 시 예외가 발생한다")
    void getRecommendedEventIdsThrowsWhenPreferenceDoesNotExist() {
        given(userPreferenceRepository.findByMemberId(1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getRecommendedEventIds(
                1L,
                "전시 추천",
                LocalDate.of(2026, 6, 20),
                LocalDate.of(2026, 6, 21),
                3
        )).isInstanceOf(RuntimeException.class);
    }


    @Test
    @DisplayName("출발 좌표가 있으면 반경 내 행사 후보를 우선 반환한다")
    void findCoordinateBasedCandidateEventsReturnsNearbyEventsFirst() {
        LocalDate startDate = LocalDate.of(2026, 6, 20);
        LocalDate endDate = LocalDate.of(2026, 6, 21);

        Event nearbyEvent = org.mockito.Mockito.mock(Event.class);
        given(nearbyEvent.getLatitude()).willReturn(37.5665);
        given(nearbyEvent.getLongitude()).willReturn(126.9780);

        given(eventRepository.findAllEventsByDate(startDate, endDate))
                .willReturn(List.of(nearbyEvent));

        @SuppressWarnings("unchecked")
        List<Event> result = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "findCoordinateBasedCandidateEvents",
                "종로구",
                Set.of(),
                Set.of(),
                startDate,
                endDate,
                37.5665,
                126.9780,
                false
        );

        assertThat(result).containsExactly(nearbyEvent);
    }

    @Test
    @DisplayName("좌표가 없는 행사는 날씨 점수를 중립값으로 처리한다")
    void calculateWeatherScoreReturnsDefaultWhenEventHasNoCoordinates() {
        Event event = org.mockito.Mockito.mock(Event.class);
        given(event.getId()).willReturn(1L);
        given(event.getLatitude()).willReturn(null);

        Double score = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "calculateWeatherScore",
                event,
                LocalDate.of(2026, 6, 20)
        );

        assertThat(score).isEqualTo(0.5);
        verify(weatherForecastService, never()).getRepresentativeForecast(
                any(LocalDate.class), any(Double.class), any(Double.class)
        );
    }

    @Test
    @DisplayName("좌표가 없으면 혼잡도 점수를 중립값으로 처리한다")
    void calculateCrowdScoreReturnsDefaultWhenEventHasNoCoordinates() {
        Event event = org.mockito.Mockito.mock(Event.class);
        given(event.getLatitude()).willReturn(null);

        Double score = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "calculateCrowdScore",
                event
        );

        assertThat(score).isEqualTo(0.5);
        verify(nearestCrowdAreaService, never()).findNearest(any(Double.class), any(Double.class));
    }

    @Test
    @DisplayName("최대 선호 점수가 0 이하이면 정규화 점수는 0이다")
    void normalizePreferenceScoreReturnsZeroWhenMaxScoreIsNotPositive() {
        Double score = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "normalizePreferenceScore",
                3.0,
                0.0
        );

        assertThat(score).isZero();
    }

    @Test
    @DisplayName("행사 좌표 또는 현재 좌표가 없으면 거리 점수는 0이다")
    void calculateDistanceScoreReturnsZeroWhenCoordinatesAreMissing() {
        Event event = org.mockito.Mockito.mock(Event.class);

        Double score = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "calculateDistanceScore",
                event,
                null,
                126.9780
        );

        assertThat(score).isZero();
    }

    @Test
    @DisplayName("성수 외에도 홍대 입력 지역은 마포구로 정규화한다")
    void normalizeRecommendationAreaConvertsHongdaeToMapoGu() {
        String normalizedArea = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "normalizeRecommendationArea",
                "서울특별시 홍대"
        );

        assertThat(normalizedArea).isEqualTo("마포구");
    }
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("텍스트가 없거나 공백뿐이면 hasText는 false를 반환한다")
    void hasTextReturnsFalseForNullOrBlank(String value) {
        Boolean result = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "hasText",
                value
        );

        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"전시", "  공연  ", "서울 나들이"})
    @DisplayName("내용이 있는 텍스트면 hasText는 true를 반환한다")
    void hasTextReturnsTrueForMeaningfulText(String value) {
        Boolean result = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "hasText",
                value
        );

        assertThat(result).isTrue();
    }


    @ParameterizedTest
    @CsvSource({
            "종로구, 종로구",
            "성수, 성동구",
            "서울특별시 성수, 성동구",
            "홍대, 마포구",
            "서울특별시 홍대, 마포구"
    })
    @DisplayName("대표 지역명은 추천 조회용 자치구명으로 정규화한다")
    void normalizeRecommendationAreaNormalizesKnownAreas(String input, String expected) {
        String normalizedArea = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "normalizeRecommendationArea",
                input
        );

        assertThat(normalizedArea).isEqualTo(expected);
    }

    @Test
    @DisplayName("최대 선호 점수가 양수이면 원점수 비율로 정규화한다")
    void normalizePreferenceScoreDividesByPositiveMaxScore() {
        Double score = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "normalizePreferenceScore",
                3.0,
                6.0
        );

        assertThat(score).isEqualTo(0.5);
    }

    @Test
    @DisplayName("행사와 현재 위치 좌표가 모두 있으면 거리 점수는 0보다 크다")
    void calculateDistanceScoreReturnsPositiveValueWhenCoordinatesExist() {
        Event event = org.mockito.Mockito.mock(Event.class);
        given(event.getLatitude()).willReturn(37.5665);
        given(event.getLongitude()).willReturn(126.9780);

        Double score = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "calculateDistanceScore",
                event,
                37.5665,
                126.9780
        );

        assertThat(score).isPositive();
    }

    @Test
    @DisplayName("선택 지역이 다르고 카테고리 조건도 없으면 기본 추천 이유를 생성한다")
    void createRecommendationReasonCreatesDefaultReasonWithoutMatchingConditions() {
        Category eventCategory = org.mockito.Mockito.mock(Category.class);
        given(eventCategory.getId()).willReturn(999L);
        given(eventCategory.getName()).willReturn("공연");

        Event event = org.mockito.Mockito.mock(Event.class);
        given(event.getArea()).willReturn("강남구");
        given(event.getCategory()).willReturn(eventCategory);

        String reason = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "createRecommendationReason",
                event,
                "종로구",
                Set.of(),
                Set.of()
        );

        assertThat(reason).isNotBlank();
    }
    @Test
    @DisplayName("여러 행사 후보가 있으면 행사 랭킹 경로를 수행하고 topK 범위의 결과를 반환한다")
    void getRecommendedEventIdsExecutesRankingForMultipleCandidates() {
        LocalDate startDate = LocalDate.of(2026, 6, 20);
        LocalDate endDate = LocalDate.of(2026, 6, 21);
        Set<java.time.DayOfWeek> requestedDays = Set.of(
                java.time.DayOfWeek.SATURDAY,
                java.time.DayOfWeek.SUNDAY
        );

        Category category = org.mockito.Mockito.mock(Category.class);
        lenient().when(category.getId()).thenReturn(201L);
        lenient().when(category.getName()).thenReturn("전시");

        Event preferredEvent = org.mockito.Mockito.mock(Event.class);
        lenient().when(preferredEvent.getId()).thenReturn(1L);
        lenient().when(preferredEvent.getArea()).thenReturn("종로구");
        lenient().when(preferredEvent.getCategory()).thenReturn(category);
        lenient().when(preferredEvent.getEventCategory()).thenReturn("전시");
        lenient().when(preferredEvent.getTarget()).thenReturn("누구나");
        lenient().when(preferredEvent.getTitle()).thenReturn("가까운 전시");
        lenient().when(preferredEvent.getEventTime()).thenReturn("상시");
        lenient().when(preferredEvent.getLatitude()).thenReturn(null);

        Event anotherEvent = org.mockito.Mockito.mock(Event.class);
        lenient().when(anotherEvent.getId()).thenReturn(2L);
        lenient().when(anotherEvent.getArea()).thenReturn("종로구");
        lenient().when(anotherEvent.getCategory()).thenReturn(category);
        lenient().when(anotherEvent.getEventCategory()).thenReturn("전시");
        lenient().when(anotherEvent.getTarget()).thenReturn("누구나");
        lenient().when(anotherEvent.getTitle()).thenReturn("다른 전시");
        lenient().when(anotherEvent.getEventTime()).thenReturn("상시");
        lenient().when(anotherEvent.getLatitude()).thenReturn(null);

        given(eventRepository.findRecommendedEvents("종로구", startDate, endDate))
                .willReturn(List.of(preferredEvent, anotherEvent));
        given(eventScheduleMatcher.getDaysOfWeekInPeriod(startDate, endDate))
                .willReturn(requestedDays);
        given(eventScheduleMatcher.isEventAvailableOnDays(
                anyString(),
                any(LocalDate.class),
                any(LocalDate.class),
                any()
        )).willReturn(true);

        List<Long> result = recommendationService.getRecommendedEventIds(
                "종로구",
                Set.of(),
                "전시 추천",
                startDate,
                endDate,
                null,
                null,
                false,
                1
        );

        assertThat(result).isNotNull();
        assertThat(result).hasSizeLessThanOrEqualTo(1);
        verify(eventRepository).findRecommendedEvents("종로구", startDate, endDate);
        verify(eventScheduleMatcher).getDaysOfWeekInPeriod(startDate, endDate);
        verify(eventScheduleMatcher, org.mockito.Mockito.times(2)).isEventAvailableOnDays(
                anyString(),
                any(LocalDate.class),
                any(LocalDate.class),
                any()
        );
    }

    @Test
    @DisplayName("관광지 카테고리가 없으면 관광지 후보를 생성하지 않는다")
    void createTourCandidatesReturnsEmptyWhenTourCategoriesAreMissing() {
        @SuppressWarnings("unchecked")
        List<?> result = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "createTourCandidates",
                List.of(),
                "종로구 전시 관광",
                37.5665,
                126.9780
        );

        assertThat(result).isEmpty();
        verify(tourRepository, never()).findActiveToursByCat3WithinBounds(
                any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()
        );
    }

    @Test
    @DisplayName("출발 좌표가 없으면 관광지 후보를 생성하지 않는다")
    void createTourCandidatesReturnsEmptyWhenStartLatitudeIsMissing() {
        @SuppressWarnings("unchecked")
        List<?> result = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "createTourCandidates",
                List.of("A0201"),
                "종로구 전시 관광",
                null,
                126.9780
        );

        assertThat(result).isEmpty();
        verify(tourRepository, never()).findActiveToursByCat3WithinBounds(
                any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()
        );
    }
    @Test
    @DisplayName("반경 조회 시 좌표가 없거나 반경 밖인 관광지는 제외한다")
    void findToursWithinRadiusFiltersInvalidAndFarTours() {
        Tour nearbyTour = org.mockito.Mockito.mock(Tour.class);
        given(nearbyTour.getLatitude()).willReturn(37.5665);
        given(nearbyTour.getLongitude()).willReturn(126.9780);

        Tour farTour = org.mockito.Mockito.mock(Tour.class);
        given(farTour.getLatitude()).willReturn(37.7000);
        given(farTour.getLongitude()).willReturn(127.2000);

        Tour tourWithoutCoordinate = org.mockito.Mockito.mock(Tour.class);
        given(tourWithoutCoordinate.getLatitude()).willReturn(null);

        given(tourRepository.findActiveToursByCat3WithinBounds(
                any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).willReturn(List.of(nearbyTour, farTour, tourWithoutCoordinate));

        @SuppressWarnings("unchecked")
        List<Tour> result = ReflectionTestUtils.invokeMethod(
                recommendationService,
                "findToursWithinRadius",
                List.of("A0201"),
                37.5665,
                126.9780,
                3.0
        );

        assertThat(result).containsExactly(nearbyTour);
    }

    @Test
    @DisplayName("관광지 후보는 검색 유사도와 거리 점수를 반영해 TOUR 후보로 변환한다")
    void rankTourCandidatesCreatesRankedTourCandidate() {
        Category category = org.mockito.Mockito.mock(Category.class);
        given(category.getName()).willReturn("문화시설");

        Tour tour = org.mockito.Mockito.mock(Tour.class);
        given(tour.getId()).willReturn(10L);
        given(tour.getArea()).willReturn("종로구");
        given(tour.getCategory()).willReturn(category);
        given(tour.getDetailCategoryName()).willReturn("박물관");
        given(tour.getTitle()).willReturn("테스트 박물관");
        given(tour.getAddress()).willReturn("서울 종로구");
        given(tour.getOverview()).willReturn("관광지 설명");
        given(tour.getLatitude()).willReturn(37.5665);
        given(tour.getLongitude()).willReturn(126.9780);
        given(tour.getEmbeddingVector()).willReturn(new float[]{1.0f, 0.0f});

        given(searchUtils.tokenize(anyString())).willReturn(List.of("박물관"));
        given(vectorEngine.getEmbedding(anyString())).willReturn(new float[]{1.0f, 0.0f});
        given(searchUtils.calculateBM25(any(), any())).willReturn(java.util.Map.of(10L, 1.0));
        given(searchUtils.cosineSimilarity(any(float[].class), any(float[].class))).willReturn(1.0);

        @SuppressWarnings("unchecked")
        List<RecommendationService.RecommendationCandidate> result =
                ReflectionTestUtils.invokeMethod(
                        recommendationService,
                        "rankTourCandidates",
                        List.of(tour),
                        "종로구 박물관 관광",
                        37.5665,
                        126.9780
                );

        assertThat(result).hasSize(1);
        RecommendationService.RecommendationCandidate candidate = result.getFirst();
        assertThat(candidate.type()).isEqualTo(RecommendationService.CandidateType.TOUR);
        assertThat(candidate.eventId()).isNull();
        assertThat(candidate.tourId()).isEqualTo(10L);
        assertThat(candidate.title()).isEqualTo("테스트 박물관");
        assertThat(candidate.score()).isPositive();
    }

    @Test
    @DisplayName("가까운 관광지가 없으면 다음 반경으로 확장해 관광지 후보를 생성한다")
    void createTourCandidatesExpandsRadiusAndReturnsRankedTours() {
        Category category = org.mockito.Mockito.mock(Category.class);
        lenient().when(category.getName()).thenReturn("문화시설");

        Tour tour = org.mockito.Mockito.mock(Tour.class);
        lenient().when(tour.getId()).thenReturn(20L);
        lenient().when(tour.getArea()).thenReturn("종로구");
        lenient().when(tour.getCategory()).thenReturn(category);
        lenient().when(tour.getDetailCategoryName()).thenReturn("전시관");
        lenient().when(tour.getTitle()).thenReturn("반경 확장 관광지");
        lenient().when(tour.getAddress()).thenReturn("서울 종로구");
        lenient().when(tour.getOverview()).thenReturn("관광지 설명");
        lenient().when(tour.getLatitude()).thenReturn(37.5800);
        lenient().when(tour.getLongitude()).thenReturn(126.9900);
        lenient().when(tour.getEmbeddingVector()).thenReturn(new float[]{1.0f, 0.0f});

        given(tourRepository.findActiveToursByCat3WithinBounds(
                any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).willReturn(List.of(), List.of(tour));
        given(searchUtils.tokenize(anyString())).willReturn(List.of("관광지"));
        given(vectorEngine.getEmbedding(anyString())).willReturn(new float[]{1.0f, 0.0f});
        given(searchUtils.calculateBM25(any(), any())).willReturn(java.util.Map.of(20L, 1.0));
        given(searchUtils.cosineSimilarity(any(float[].class), any(float[].class))).willReturn(1.0);

        @SuppressWarnings("unchecked")
        List<RecommendationService.RecommendationCandidate> result =
                ReflectionTestUtils.invokeMethod(
                        recommendationService,
                        "createTourCandidates",
                        List.of("A0201"),
                        "종로구 전시관 관광",
                        37.5665,
                        126.9780
                );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().tourId()).isEqualTo(20L);
        verify(tourRepository, org.mockito.Mockito.times(2)).findActiveToursByCat3WithinBounds(
                any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()
        );
    }
    @Test
    @DisplayName("추천 요청 조건으로 행사 후보를 생성하고 AI 추천 사유를 포함한 초안을 반환한다")
    void recommendCandidatesReturnsDraftWithEventCandidateAndAiReasons() {
        Long memberId = 1L;
        LocalDate startDate = LocalDate.of(2026, 6, 20);
        LocalDate endDate = LocalDate.of(2026, 6, 21);

        RecommendationCourseCreateRequest request = org.mockito.Mockito.mock(
                RecommendationCourseCreateRequest.class
        );
        given(request.categories()).willReturn(List.of("전시"));
        given(request.area()).willReturn("종로구");
        given(request.companionType()).willReturn("COUPLE");
        given(request.startDate()).willReturn(startDate);
        given(request.endDate()).willReturn(endDate);
        given(request.latitude()).willReturn(null);
        given(request.longitude()).willReturn(null);

        Category category = org.mockito.Mockito.mock(Category.class);
        given(category.getId()).willReturn(201L);
        given(category.getName()).willReturn("전시");

        Event event = org.mockito.Mockito.mock(Event.class);
        given(event.getId()).willReturn(100L);
        given(event.getArea()).willReturn("종로구");
        given(event.getCategory()).willReturn(category);
        given(event.getEventCategory()).willReturn("전시");
        given(event.getTarget()).willReturn("커플");
        given(event.getTitle()).willReturn("테스트 전시");
        given(event.getEventTime()).willReturn("상시");
        given(event.getLatitude()).willReturn(null);
        given(event.getEmbeddingVector()).willReturn(new float[]{1.0f, 0.0f});
        given(event.getPlace()).willReturn(null);

        given(userPreferenceRepository.findByMemberId(memberId)).willReturn(Optional.empty());
        given(preferenceEventCategoryMappingRepository
                .findEventCategoryIdsByPreferenceCategoryNames(List.of("전시")))
                .willReturn(List.of(201L));
        given(eventRepository.findRecommendedEventsWithCategoryIds(
                "종로구", startDate, endDate, Set.of(201L)
        )).willReturn(List.of(event));
        given(eventRepository.findRecommendedEvents("종로구", startDate, endDate))
                .willReturn(List.of(event));
        given(eventScheduleMatcher.getDaysOfWeekInPeriod(startDate, endDate))
                .willReturn(Set.of(java.time.DayOfWeek.SATURDAY));
        given(eventScheduleMatcher.isEventAvailableOnDays(
                anyString(), any(LocalDate.class), any(LocalDate.class), any()
        )).willReturn(true);
        given(searchUtils.tokenize(anyString())).willReturn(List.of("전시"));
        given(vectorEngine.getEmbedding(anyString())).willReturn(new float[]{1.0f, 0.0f});
        given(searchUtils.calculateBM25(any(), any())).willReturn(java.util.Map.of(100L, 1.0));
        given(searchUtils.cosineSimilarity(any(float[].class), any(float[].class))).willReturn(1.0);
        given(preferenceTourCategoryMappingRepository
                .findTourCat3CodesByPreferenceCategoryNames(List.of("전시")))
                .willReturn(List.of());
        given(aiRecommendationReasonService.generatePlaceReasons(any()))
                .willReturn(List.of("AI 장소 추천 이유"));
        given(aiRecommendationReasonService.generateCourseReason(any()))
                .willReturn("AI 코스 추천 이유");

        RecommendationService.RecommendationCandidateDraft result =
                recommendationService.recommendCandidates(memberId, request);

        assertThat(result.startDate()).isEqualTo(startDate);
        assertThat(result.endDate()).isEqualTo(endDate);
        assertThat(result.baseArea()).isEqualTo("종로구");
        assertThat(result.companionType()).isEqualTo("COUPLE");
        assertThat(result.recommendationReason()).isEqualTo("AI 코스 추천 이유");
        assertThat(result.candidates()).hasSize(1);
        assertThat(result.candidates().getFirst().type())
                .isEqualTo(RecommendationService.CandidateType.EVENT);
        assertThat(result.candidates().getFirst().eventId()).isEqualTo(100L);
        assertThat(result.candidates().getFirst().recommendationReason())
                .isEqualTo("AI 장소 추천 이유");
        verify(aiRecommendationReasonService).generatePlaceReasons(any());
        verify(aiRecommendationReasonService).generateCourseReason(any());
    }

    @Test
    @DisplayName("행사와 관광지 후보가 모두 없으면 통합 추천 후보 생성 시 예외가 발생한다")
    void recommendCandidatesThrowsWhenNoEventAndTourCandidatesExist() {
        Long memberId = 1L;
        LocalDate startDate = LocalDate.of(2026, 6, 20);
        LocalDate endDate = LocalDate.of(2026, 6, 21);

        RecommendationCourseCreateRequest request = org.mockito.Mockito.mock(
                RecommendationCourseCreateRequest.class
        );
        given(request.categories()).willReturn(List.of());
        given(request.area()).willReturn("종로구");
        given(request.companionType()).willReturn("COUPLE");
        given(request.startDate()).willReturn(startDate);
        given(request.endDate()).willReturn(endDate);
        given(request.latitude()).willReturn(null);
        given(request.longitude()).willReturn(null);

        given(userPreferenceRepository.findByMemberId(memberId)).willReturn(Optional.empty());
        given(eventRepository.findRecommendedEvents("종로구", startDate, endDate))
                .willReturn(List.of());
        given(eventRepository.findRecommendedEvents(
                "종로구", startDate.minusDays(7), endDate.plusDays(7)
        )).willReturn(List.of());
        given(eventRepository.findAllEventsByDate(startDate, endDate)).willReturn(List.of());

        assertThatThrownBy(() -> recommendationService.recommendCandidates(memberId, request))
                .isInstanceOf(RuntimeException.class);

        verify(aiRecommendationReasonService, never()).generatePlaceReasons(any());
        verify(aiRecommendationReasonService, never()).generateCourseReason(any());
    }
}