
package come.back.gotoday.weather.service;

import come.back.gotoday.weather.model.WeatherCondition;
import org.springframework.stereotype.Component;

/**
 * 날씨 상태와 행사 실내·실외 특성을 기준으로 추천용 날씨 적합도 점수를 계산합니다.
 *
 * 반환 점수 범위는 0.0 ~ 1.0이며, 1.0에 가까울수록 해당 날씨에 적합한 행사입니다.
 */
@Component
public class WeatherScoreCalculator {

    private static final double NEUTRAL_SCORE = 0.5;
    private static final double FAVORABLE_SCORE = 0.9;
    private static final double UNFAVORABLE_SCORE = 0.2;
    private static final double CAUTION_SCORE = 0.4;
    private static final double INDOOR_PREFERRED_SCORE = 0.8;
    private static final double CLOUDY_OUTDOOR_SCORE = 0.7;

    /**
     * 날씨 상태와 실내 여부를 기준으로 행사 적합도 점수를 반환합니다.
     *
     * @param weatherCondition 단순화된 날씨 상태
     * @param indoorEvent 실내 행사 여부. 판단할 수 없으면 null
     * @return 0.0 ~ 1.0 범위의 날씨 적합도 점수
     */
    public double calculate(WeatherCondition weatherCondition, Boolean indoorEvent) {
        if (weatherCondition == null || weatherCondition == WeatherCondition.UNKNOWN || indoorEvent == null) {
            return NEUTRAL_SCORE;
        }

        return switch (weatherCondition) {
            case RAIN, SNOW -> indoorEvent ? FAVORABLE_SCORE : UNFAVORABLE_SCORE;
            case HOT, COLD, STRONG_WIND -> indoorEvent ? INDOOR_PREFERRED_SCORE : CAUTION_SCORE;
            case CLEAR -> indoorEvent ? NEUTRAL_SCORE : FAVORABLE_SCORE;
            case CLOUDY -> indoorEvent ? NEUTRAL_SCORE : CLOUDY_OUTDOOR_SCORE;
            case UNKNOWN -> NEUTRAL_SCORE;
        };
    }
}
