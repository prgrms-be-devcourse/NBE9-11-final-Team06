
package come.back.gotoday.weather.service;

import come.back.gotoday.weather.model.WeatherCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("날씨 적합도 점수 계산기 단위 테스트")
class WeatherScoreCalculatorTest {

    private final WeatherScoreCalculator weatherScoreCalculator = new WeatherScoreCalculator();

    @Test
    @DisplayName("날씨 상태가 없으면 중립 점수 0.5를 반환한다")
    void calculateReturnsNeutralScoreWhenWeatherConditionIsNull() {
        double result = weatherScoreCalculator.calculate(null, true);

        assertThat(result).isEqualTo(0.5);
    }

    @Test
    @DisplayName("실내외 여부를 알 수 없으면 중립 점수 0.5를 반환한다")
    void calculateReturnsNeutralScoreWhenIndoorEventIsUnknown() {
        double result = weatherScoreCalculator.calculate(WeatherCondition.CLEAR, null);

        assertThat(result).isEqualTo(0.5);
    }

    @Test
    @DisplayName("UNKNOWN 날씨는 중립 점수 0.5를 반환한다")
    void calculateReturnsNeutralScoreWhenWeatherConditionIsUnknown() {
        double result = weatherScoreCalculator.calculate(WeatherCondition.UNKNOWN, false);

        assertThat(result).isEqualTo(0.5);
    }

    @Test
    @DisplayName("비 또는 눈일 때 실내 행사는 0.9점을 반환한다")
    void calculateReturnsFavorableScoreForIndoorEventWhenRainOrSnow() {
        assertThat(weatherScoreCalculator.calculate(WeatherCondition.RAIN, true))
                .isEqualTo(0.9);
        assertThat(weatherScoreCalculator.calculate(WeatherCondition.SNOW, true))
                .isEqualTo(0.9);
    }

    @Test
    @DisplayName("비 또는 눈일 때 실외 행사는 0.2점을 반환한다")
    void calculateReturnsUnfavorableScoreForOutdoorEventWhenRainOrSnow() {
        assertThat(weatherScoreCalculator.calculate(WeatherCondition.RAIN, false))
                .isEqualTo(0.2);
        assertThat(weatherScoreCalculator.calculate(WeatherCondition.SNOW, false))
                .isEqualTo(0.2);
    }

    @Test
    @DisplayName("폭염, 한파, 강풍일 때 실내 행사는 0.8점을 반환한다")
    void calculateReturnsIndoorPreferredScoreForIndoorEventWhenSevereWeather() {
        assertThat(weatherScoreCalculator.calculate(WeatherCondition.HOT, true))
                .isEqualTo(0.8);
        assertThat(weatherScoreCalculator.calculate(WeatherCondition.COLD, true))
                .isEqualTo(0.8);
        assertThat(weatherScoreCalculator.calculate(WeatherCondition.STRONG_WIND, true))
                .isEqualTo(0.8);
    }

    @Test
    @DisplayName("폭염, 한파, 강풍일 때 실외 행사는 0.4점을 반환한다")
    void calculateReturnsCautionScoreForOutdoorEventWhenSevereWeather() {
        assertThat(weatherScoreCalculator.calculate(WeatherCondition.HOT, false))
                .isEqualTo(0.4);
        assertThat(weatherScoreCalculator.calculate(WeatherCondition.COLD, false))
                .isEqualTo(0.4);
        assertThat(weatherScoreCalculator.calculate(WeatherCondition.STRONG_WIND, false))
                .isEqualTo(0.4);
    }

    @Test
    @DisplayName("맑은 날 실외 행사는 0.9점을 반환한다")
    void calculateReturnsFavorableScoreForOutdoorEventWhenClear() {
        double result = weatherScoreCalculator.calculate(WeatherCondition.CLEAR, false);

        assertThat(result).isEqualTo(0.9);
    }

    @Test
    @DisplayName("맑은 날 실내 행사는 중립 점수 0.5를 반환한다")
    void calculateReturnsNeutralScoreForIndoorEventWhenClear() {
        double result = weatherScoreCalculator.calculate(WeatherCondition.CLEAR, true);

        assertThat(result).isEqualTo(0.5);
    }

    @Test
    @DisplayName("흐린 날 실외 행사는 0.7점을 반환한다")
    void calculateReturnsOutdoorScoreWhenCloudy() {
        double result = weatherScoreCalculator.calculate(WeatherCondition.CLOUDY, false);

        assertThat(result).isEqualTo(0.7);
    }

    @Test
    @DisplayName("흐린 날 실내 행사는 중립 점수 0.5를 반환한다")
    void calculateReturnsNeutralScoreForIndoorEventWhenCloudy() {
        double result = weatherScoreCalculator.calculate(WeatherCondition.CLOUDY, true);

        assertThat(result).isEqualTo(0.5);
    }
}
