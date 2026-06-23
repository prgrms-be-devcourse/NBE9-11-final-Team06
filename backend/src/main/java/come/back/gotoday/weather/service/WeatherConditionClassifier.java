package come.back.gotoday.weather.service;

import come.back.gotoday.weather.model.WeatherCondition;
import org.springframework.stereotype.Component;

/**
 * 기상청 단기예보 원본 값을 추천용 날씨 상태로 분류합니다.
 *
 * 분류 우선순위는 강수 형태, 풍속, 기온, 하늘 상태 순서입니다.
 */
@Component
public class WeatherConditionClassifier {

    private static final double HOT_TEMPERATURE_CELSIUS = 33.0;
    private static final double COLD_TEMPERATURE_CELSIUS = 0.0;
    private static final double STRONG_WIND_SPEED_METERS_PER_SECOND = 8.0;

    /**
     * 단기예보를 추천에 사용할 단순 날씨 상태로 변환합니다.
     *
     * @param forecast 기상청 단기예보에서 추출한 대표 예보
     * @return 분류된 날씨 상태
     */
    public WeatherCondition classify(WeatherForecastService.WeatherForecast forecast) {
        if (forecast == null) {
            return WeatherCondition.UNKNOWN;
        }

        if (isSnow(forecast.precipitationType())) {
            return WeatherCondition.SNOW;
        }

        if (isRain(forecast.precipitationType())) {
            return WeatherCondition.RAIN;
        }

        if (forecast.windSpeed() >= STRONG_WIND_SPEED_METERS_PER_SECOND) {
            return WeatherCondition.STRONG_WIND;
        }

        if (forecast.temperature() >= HOT_TEMPERATURE_CELSIUS) {
            return WeatherCondition.HOT;
        }

        if (forecast.temperature() <= COLD_TEMPERATURE_CELSIUS) {
            return WeatherCondition.COLD;
        }

        return switch (forecast.skyStatus()) {
            case 1 -> WeatherCondition.CLEAR;
            case 3, 4 -> WeatherCondition.CLOUDY;
            default -> WeatherCondition.UNKNOWN;
        };
    }

    private boolean isRain(int precipitationType) {
        return precipitationType == 1 || precipitationType == 4;
    }

    private boolean isSnow(int precipitationType) {
        return precipitationType == 2 || precipitationType == 3;
    }
}
