package come.back.gotoday.crowd.service;

import come.back.gotoday.crowd.entity.CongestionLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrowdScoreCalculatorTest {

    private final CrowdScoreCalculator crowdScoreCalculator = new CrowdScoreCalculator(30, 10, -10, -30);

    @Test
    @DisplayName("혼잡도 단계가 여유이면 30점을 반환한다")
    void calculateRelaxedScore() {
        int score = crowdScoreCalculator.calculate(CongestionLevel.RELAXED);

        assertThat(score).isEqualTo(30);
    }

    @Test
    @DisplayName("혼잡도 단계가 보통이면 10점을 반환한다")
    void calculateNormalScore() {
        int score = crowdScoreCalculator.calculate(CongestionLevel.NORMAL);

        assertThat(score).isEqualTo(10);
    }

    @Test
    @DisplayName("혼잡도 단계가 약간 붐빔이면 -10점을 반환한다")
    void calculateCrowdedScore() {
        int score = crowdScoreCalculator.calculate(CongestionLevel.CROWDED);

        assertThat(score).isEqualTo(-10);
    }

    @Test
    @DisplayName("혼잡도 단계가 붐빔이면 -30점을 반환한다")
    void calculateVeryCrowdedScore() {
        int score = crowdScoreCalculator.calculate(CongestionLevel.VERY_CROWDED);

        assertThat(score).isEqualTo(-30);
    }

    @Test
    @DisplayName("혼잡도 단계가 없으면 0점을 반환한다")
    void calculateNullScore() {
        int score = crowdScoreCalculator.calculate(null);

        assertThat(score).isEqualTo(0);
    }
}