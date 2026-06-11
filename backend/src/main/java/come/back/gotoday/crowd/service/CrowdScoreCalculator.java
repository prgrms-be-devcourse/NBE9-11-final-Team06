package come.back.gotoday.crowd.service;

import come.back.gotoday.crowd.entity.CongestionLevel;
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

    /**
     * 혼잡도 단계에 따라 추천 점수를 계산합니다.
     *
     * @param congestionLevel 혼잡도 단계
     * @return 추천 점수에 반영할 혼잡도 점수
     */
    public int calculate(CongestionLevel congestionLevel) {
        if (congestionLevel == null) {
            return 0;
        }

        return switch (congestionLevel) {
            case RELAXED -> 30;
            case NORMAL -> 10;
            case CROWDED -> -10;
            case VERY_CROWDED -> -30;
        };
    }
}
