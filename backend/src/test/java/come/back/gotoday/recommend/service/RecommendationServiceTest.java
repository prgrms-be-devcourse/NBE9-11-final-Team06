package come.back.gotoday.recommend.service;

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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("추천 서비스 단위 테스트")
class RecommendationServiceTest {

    @Mock
    private NearestCrowdAreaService nearestCrowdAreaService;

    @Mock
    private CrowdScoreCalculator crowdScoreCalculator;

    @InjectMocks
    private RecommendationService recommendationService;

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
    @DisplayName("혼잡한 곳 피하기를 선택하면 쾌적한 행사가 우선된다")
    void selectGreedyEventIdsAvoidCrowdsPrioritizesRelaxedEvent() {
        Event crowdedEvent = mockEvent(1L, 37.5000, 127.0000);
        Event relaxedEvent = mockEvent(2L, 37.5000, 127.0000);

        given(nearestCrowdAreaService.findNearest(37.5000, 127.0000))
                .willReturn(
                        Optional.of(nearestArea(CongestionLevel.VERY_CROWDED)),
                        Optional.of(nearestArea(CongestionLevel.RELAXED))
                );
        given(crowdScoreCalculator.calculate(CongestionLevel.VERY_CROWDED)).willReturn(0);
        given(crowdScoreCalculator.calculate(CongestionLevel.RELAXED)).willReturn(100);

        List<Long> result = selectGreedyEventIds(
                List.of(crowdedEvent, relaxedEvent),
                Map.of(1L, 1.0, 2L, 1.0),
                37.5000,
                127.0000,
                true,
                2
        );

        assertThat(result).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("혼잡도 상관없음을 선택하면 혼잡도 조회를 생략한다")
    void selectGreedyEventIdsIndifferentSkipsCrowdLookup() {
        Event firstEvent = mockEvent(1L, 37.5000, 127.0000);
        Event secondEvent = mockEvent(2L, 37.5100, 127.0100);

        List<Long> result = selectGreedyEventIds(
                List.of(firstEvent, secondEvent),
                Map.of(1L, 1.0, 2L, 0.5),
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
    @DisplayName("첫 장소를 선택한 뒤 해당 좌표를 기준으로 다음 가까운 행사를 선택한다")
    void selectGreedyEventIdsUpdatesCurrentLocation() {
        Event firstEvent = mockEvent(1L, 37.5000, 127.0000);
        Event nearFirstEvent = mockEvent(2L, 37.5005, 127.0005);
        Event farFromFirstEvent = mockEvent(3L, 37.5500, 127.0500);

        List<Long> result = selectGreedyEventIds(
                List.of(firstEvent, nearFirstEvent, farFromFirstEvent),
                Map.of(1L, 1.0, 2L, 1.0, 3L, 1.0),
                37.5000,
                127.0000,
                false,
                3
        );

        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("선택된 행사는 중복 선택하지 않고 후보 수만큼만 반환한다")
    void selectGreedyEventIdsDoesNotSelectDuplicates() {
        Event firstEvent = mockEvent(1L, 37.5000, 127.0000);
        Event secondEvent = mockEvent(2L, 37.5100, 127.0100);

        List<Long> result = selectGreedyEventIds(
                List.of(firstEvent, secondEvent),
                Map.of(1L, 1.0, 2L, 0.8),
                37.5000,
                127.0000,
                false,
                5
        );

        assertThat(result)
                .containsExactly(1L, 2L)
                .doesNotHaveDuplicates();
    }

    @SuppressWarnings("unchecked")
    private List<Long> selectGreedyEventIds(
            List<Event> candidateEvents,
            Map<Long, Double> preferenceScores,
            Double startLatitude,
            Double startLongitude,
            boolean avoidCrowds,
            int topK
    ) {
        return (List<Long>) ReflectionTestUtils.invokeMethod(
                recommendationService,
                "selectGreedyEventIds",
                candidateEvents,
                preferenceScores,
                startLatitude,
                startLongitude,
                avoidCrowds,
                topK
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