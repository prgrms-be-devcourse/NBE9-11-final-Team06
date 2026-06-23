package come.back.gotoday.weather.service;

import come.back.gotoday.weather.model.WeatherCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("날씨 상태 분류기 단위 테스트")
class WeatherConditionClassifierTest {

    private final WeatherConditionClassifier weatherConditionClassifier = new WeatherConditionClassifier();

    @Test
    @DisplayName("예보가 없으면 UNKNOWN을 반환한다")
    void classifyReturnsUnknownWhenForecastIsNull() {
        WeatherCondition result = weatherConditionClassifier.classify(null);

        assertThat(result).isEqualTo(WeatherCondition.UNKNOWN);
    }

    @Test
    @DisplayName("강수 형태가 눈이면 SNOW를 반환한다")
    void classifyReturnsSnowWhenPrecipitationTypeIsSnow() {
        WeatherForecastService.WeatherForecast forecast = forecast(3, 1, 20.0, 1.0);

        WeatherCondition result = weatherConditionClassifier.classify(forecast);

        assertThat(result).isEqualTo(WeatherCondition.SNOW);
    }

    @Test
    @DisplayName("강수 형태가 비면 RAIN을 반환한다")
    void classifyReturnsRainWhenPrecipitationTypeIsRain() {
        WeatherForecastService.WeatherForecast forecast = forecast(1, 1, 20.0, 1.0);

        WeatherCondition result = weatherConditionClassifier.classify(forecast);

        assertThat(result).isEqualTo(WeatherCondition.RAIN);
    }

    @Test
    @DisplayName("눈과 비 조건이 함께 있으면 눈을 우선 반환한다")
    void classifyPrioritizesSnowOverOtherConditions() {
        WeatherForecastService.WeatherForecast forecast = forecast(2, 1, 35.0, 10.0);

        WeatherCondition result = weatherConditionClassifier.classify(forecast);

        assertThat(result).isEqualTo(WeatherCondition.SNOW);
    }

    @Test
    @DisplayName("풍속이 8m/s 이상이면 STRONG_WIND를 반환한다")
    void classifyReturnsStrongWindWhenWindSpeedIsHigh() {
        WeatherForecastService.WeatherForecast forecast = forecast(0, 1, 20.0, 8.0);

        WeatherCondition result = weatherConditionClassifier.classify(forecast);

        assertThat(result).isEqualTo(WeatherCondition.STRONG_WIND);
    }

    @Test
    @DisplayName("기온이 33도 이상이면 HOT을 반환한다")
    void classifyReturnsHotWhenTemperatureIsHigh() {
        WeatherForecastService.WeatherForecast forecast = forecast(0, 1, 33.0, 1.0);

        WeatherCondition result = weatherConditionClassifier.classify(forecast);

        assertThat(result).isEqualTo(WeatherCondition.HOT);
    }

    @Test
    @DisplayName("기온이 0도 이하이면 COLD를 반환한다")
    void classifyReturnsColdWhenTemperatureIsLow() {
        WeatherForecastService.WeatherForecast forecast = forecast(0, 1, 0.0, 1.0);

        WeatherCondition result = weatherConditionClassifier.classify(forecast);

        assertThat(result).isEqualTo(WeatherCondition.COLD);
    }

    @Test
    @DisplayName("하늘 상태가 맑음이면 CLEAR를 반환한다")
    void classifyReturnsClearWhenSkyStatusIsClear() {
        WeatherForecastService.WeatherForecast forecast = forecast(0, 1, 20.0, 1.0);

        WeatherCondition result = weatherConditionClassifier.classify(forecast);

        assertThat(result).isEqualTo(WeatherCondition.CLEAR);
    }

    @Test
    @DisplayName("하늘 상태가 구름 많음 또는 흐림이면 CLOUDY를 반환한다")
    void classifyReturnsCloudyWhenSkyStatusIsCloudy() {
        WeatherForecastService.WeatherForecast cloudyForecast = forecast(0, 3, 20.0, 1.0);
        WeatherForecastService.WeatherForecast overcastForecast = forecast(0, 4, 20.0, 1.0);

        assertThat(weatherConditionClassifier.classify(cloudyForecast))
                .isEqualTo(WeatherCondition.CLOUDY);
        assertThat(weatherConditionClassifier.classify(overcastForecast))
                .isEqualTo(WeatherCondition.CLOUDY);
    }

    @Test
    @DisplayName("알 수 없는 하늘 상태면 UNKNOWN을 반환한다")
    void classifyReturnsUnknownWhenSkyStatusIsUnsupported() {
        WeatherForecastService.WeatherForecast forecast = forecast(0, 2, 20.0, 1.0);

        WeatherCondition result = weatherConditionClassifier.classify(forecast);

        assertThat(result).isEqualTo(WeatherCondition.UNKNOWN);
    }

    private WeatherForecastService.WeatherForecast forecast(
            int precipitationType,
            int skyStatus,
            double temperature,
            double windSpeed
    ) {
        return new WeatherForecastService.WeatherForecast(
                LocalDate.of(2026, 6, 22),
                LocalTime.of(14, 0),
                precipitationType,
                0,
                skyStatus,
                temperature,
                windSpeed
        );
    }
}
