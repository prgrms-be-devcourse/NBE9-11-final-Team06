package come.back.gotoday.recommend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("추천 서비스 검색어 생성 단위 테스트")
class RecommendationServiceTest {

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
}