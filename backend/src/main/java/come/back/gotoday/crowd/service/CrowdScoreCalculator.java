package come.back.gotoday.crowd.service;

import come.back.gotoday.crowd.entity.CongestionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 혼잡도 단계별 추천 점수를 계산하는 클래스입니다.
 *
 * 혼잡도가 낮은 장소는 추천 점수를 높이고,
 * 혼잡도가 높은 장소는 추천 점수를 낮춰
 * 더 쾌적한 장소가 우선 추천되도록 합니다.
 */
@Component
public class CrowdScoreCalculator {

    private final int relaxedScore;
    private final int normalScore;
    private final int crowdedScore;
    private final int veryCrowdedScore;

    public CrowdScoreCalculator(
            @Value("${recommendation.score.crowd.relaxed}") int relaxedScore,
            @Value("${recommendation.score.crowd.normal}") int normalScore,
            @Value("${recommendation.score.crowd.crowded}") int crowdedScore,
            @Value("${recommendation.score.crowd.very-crowded}") int veryCrowdedScore
    ) {
        this.relaxedScore = relaxedScore;
        this.normalScore = normalScore;
        this.crowdedScore = crowdedScore;
        this.veryCrowdedScore = veryCrowdedScore;
    }

    public int calculate(CongestionLevel congestionLevel) {
        if (congestionLevel == null) {
            return 0;
        }

        return switch (congestionLevel) {
            case RELAXED -> relaxedScore;
            case NORMAL -> normalScore;
            case CROWDED -> crowdedScore;
            case VERY_CROWDED -> veryCrowdedScore;
        };
    }
}
