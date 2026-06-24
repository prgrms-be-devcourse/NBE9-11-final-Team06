package come.back.gotoday.recommend.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.PreferenceEventCategoryMappingRepository;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.weather.service.EventIndoorOutdoorPolicy;
import come.back.gotoday.weather.service.WeatherConditionClassifier;
import come.back.gotoday.weather.service.WeatherForecastService;
import come.back.gotoday.weather.service.WeatherScoreCalculator;
import come.back.gotoday.preference.entity.UserPreference;
import come.back.gotoday.preference.repository.UserPreferenceCategoryRepository;
import come.back.gotoday.preference.repository.UserPreferenceRepository;

import come.back.gotoday.crowd.entity.CongestionLevel;
import come.back.gotoday.crowd.service.CrowdScoreCalculator;
import come.back.gotoday.crowd.service.NearestCrowdAreaService;
import come.back.gotoday.event.entity.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("추천 서비스 단위 테스트")
class RecommendationServiceTest {

    private static final LocalDate BEAM_SEARCH_DATE = LocalDate.of(2026, 6, 20);

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private UserPreferenceCategoryRepository userPreferenceCategoryRepository;

    @Mock
    private PreferenceEventCategoryMappingRepository preferenceEventCategoryMappingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NearestCrowdAreaService nearestCrowdAreaService;

    @Mock
    private CrowdScoreCalculator crowdScoreCalculator;

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
        verify(eventRepository, times(2)).findRecommendedEvents(
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
    @DisplayName("빔 서치에서 혼잡한 곳 피하기를 선택하면 쾌적한 행사가 우선된다")
    void selectBeamSearchEventIdsAvoidCrowdsPrioritizesRelaxedEvent() {
        Event crowdedEvent = mockEvent(1L, 37.5000, 127.0000);
        Event relaxedEvent = mockEvent(2L, 37.5000, 127.0000);

        given(nearestCrowdAreaService.findNearest(37.5000, 127.0000))
                .willReturn(
                        Optional.of(nearestArea(CongestionLevel.VERY_CROWDED)),
                        Optional.of(nearestArea(CongestionLevel.RELAXED))
                );
        given(crowdScoreCalculator.calculate(CongestionLevel.VERY_CROWDED)).willReturn(0);
        given(crowdScoreCalculator.calculate(CongestionLevel.RELAXED)).willReturn(100);

        List<Long> result = selectBeamSearchEventIds(
                List.of(crowdedEvent, relaxedEvent),
                Map.of(1L, 1.0, 2L, 1.0),
                BEAM_SEARCH_DATE,
                37.5000,
                127.0000,
                true,
                2
        );

        assertThat(result).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("빔 서치에서 혼잡도 상관없음을 선택하면 혼잡도 조회를 생략한다")
    void selectBeamSearchEventIdsIndifferentSkipsCrowdLookup() {
        Event firstEvent = mockEvent(1L, 37.5000, 127.0000);
        Event secondEvent = mockEvent(2L, 37.5100, 127.0100);

        List<Long> result = selectBeamSearchEventIds(
                List.of(firstEvent, secondEvent),
                Map.of(1L, 1.0, 2L, 0.5),
                BEAM_SEARCH_DATE,
                37.5000,
                127.0000,
                false,
                2
        );

        assertThat(result).containsExactly(1L, 2L);
        verify(nearestCrowdAreaService, never()).findNearest(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble()
        );
        verify(crowdScoreCalculator, never()).calculate(
                org.mockito.ArgumentMatchers.any(CongestionLevel.class)
        );
    }

    @Test
    @DisplayName("빔 서치는 직전 행사 좌표를 다음 후보 점수 계산에 사용한다")
    void selectBeamSearchEventIdsUpdatesCurrentLocation() {
        Event firstEvent = mockEvent(1L, 37.5000, 127.0000);
        Event nearFirstEvent = mockEvent(2L, 37.5005, 127.0005);
        Event farFromFirstEvent = mockEvent(3L, 37.5500, 127.0500);

        List<Long> result = selectBeamSearchEventIds(
                List.of(firstEvent, nearFirstEvent, farFromFirstEvent),
                Map.of(1L, 1.0, 2L, 1.0, 3L, 1.0),
                BEAM_SEARCH_DATE,
                37.5000,
                127.0000,
                false,
                3
        );

        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("빔 서치는 동일 행사를 중복 선택하지 않고 후보 수만큼만 반환한다")
    void selectBeamSearchEventIdsDoesNotSelectDuplicates() {
        Event firstEvent = mockEvent(1L, 37.5000, 127.0000);
        Event secondEvent = mockEvent(2L, 37.5100, 127.0100);

        List<Long> result = selectBeamSearchEventIds(
                List.of(firstEvent, secondEvent),
                Map.of(1L, 1.0, 2L, 0.8),
                BEAM_SEARCH_DATE,
                37.5000,
                127.0000,
                false,
                5
        );

        assertThat(result)
                .containsExactly(1L, 2L)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("빔 서치는 topK가 0이면 빈 결과를 반환한다")
    void selectBeamSearchEventIdsReturnsEmptyWhenTopKIsZero() {
        Event event = org.mockito.Mockito.mock(Event.class);

        List<Long> result = selectBeamSearchEventIds(
                List.of(event),
                Map.of(1L, 1.0),
                BEAM_SEARCH_DATE,
                37.5000,
                127.0000,
                false,
                0
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("빔 서치는 시작 좌표가 없어도 중복 없이 모든 후보를 추천한다")
    void selectBeamSearchEventIdsWorksWithoutStartCoordinates() {
        Event firstEvent = mockEvent(1L, 37.5000, 127.0000);
        Event secondEvent = mockEvent(2L, 37.5100, 127.0100);
        Event thirdEvent = mockEvent(3L, 37.5200, 127.0200);

        List<Long> result = selectBeamSearchEventIds(
                List.of(firstEvent, secondEvent, thirdEvent),
                Map.of(1L, 1.0, 2L, 0.8, 3L, 0.6),
                BEAM_SEARCH_DATE,
                null,
                null,
                false,
                5
        );

        assertThat(result)
                .containsExactlyInAnyOrder(1L, 2L, 3L)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("빔 서치는 선호 점수가 없는 행사를 추천 후보에서 제외한다")
    void selectBeamSearchEventIdsExcludesEventsWithoutPreferenceScore() {
        Event scoredEvent = mockEvent(1L, 37.5000, 127.0000);
        Event unscoredEvent = org.mockito.Mockito.mock(Event.class);
        given(unscoredEvent.getId()).willReturn(2L);

        List<Long> result = selectBeamSearchEventIds(
                List.of(scoredEvent, unscoredEvent),
                Map.of(1L, 1.0),
                BEAM_SEARCH_DATE,
                37.5000,
                127.0000,
                false,
                2
        );

        assertThat(result).containsExactly(1L);
    }

    @Test
    @DisplayName("빔 서치는 동일한 입력에 대해 항상 동일한 방문 순서를 반환한다")
    void selectBeamSearchEventIdsReturnsDeterministicOrder() {
        Event firstEvent = mockEvent(1L, 37.5000, 127.0000);
        Event secondEvent = mockEvent(2L, 37.5005, 127.0005);
        Event thirdEvent = mockEvent(3L, 37.5100, 127.0100);

        List<Event> candidateEvents = List.of(firstEvent, secondEvent, thirdEvent);
        Map<Long, Double> preferenceScores = Map.of(1L, 1.0, 2L, 1.0, 3L, 1.0);

        List<Long> firstResult = selectBeamSearchEventIds(
                candidateEvents,
                preferenceScores,
                BEAM_SEARCH_DATE,
                37.5000,
                127.0000,
                false,
                3
        );
        List<Long> secondResult = selectBeamSearchEventIds(
                candidateEvents,
                preferenceScores,
                BEAM_SEARCH_DATE,
                37.5000,
                127.0000,
                false,
                3
        );

        assertThat(secondResult).containsExactlyElementsOf(firstResult);
    }

    @SuppressWarnings("unchecked")
    private List<Long> selectBeamSearchEventIds(
            List<Event> candidateEvents,
            Map<Long, Double> preferenceScores,
            LocalDate searchStart,
            Double startLatitude,
            Double startLongitude,
            boolean avoidCrowds,
            int topK
    ) {
        return (List<Long>) ReflectionTestUtils.invokeMethod(
                recommendationService,
                "selectBeamSearchEventIds",
                candidateEvents,
                preferenceScores,
                searchStart,
                startLatitude,
                startLongitude,
                avoidCrowds,
                topK,
                5
        );
    }

    private Event mockEvent(Long id, Double latitude, Double longitude) {
        Event event = org.mockito.Mockito.mock(Event.class);
        given(event.getId()).willReturn(id);
        given(event.getLatitude()).willReturn(latitude);
        given(event.getLongitude()).willReturn(longitude);
        return event;
    }

    private NearestCrowdAreaService.NearestCrowdArea nearestArea(
            CongestionLevel congestionLevel
    ) {
        return new NearestCrowdAreaService.NearestCrowdArea(
                1L,
                "테스트 지역",
                "POI001",
                37.5000,
                127.0000,
                0.1,
                congestionLevel
        );
    }
}