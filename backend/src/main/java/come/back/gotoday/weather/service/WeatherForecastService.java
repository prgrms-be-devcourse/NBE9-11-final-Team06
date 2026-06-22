package come.back.gotoday.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import come.back.gotoday.external.weather.KmaWeatherClient;
import come.back.gotoday.weather.util.KmaGridConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 기상청 단기예보 응답에서 코스 추천에 사용할 대표 날씨 정보를 추출하는 서비스입니다.
 *
 * 현재 MVP에서는 사용자가 선택한 날짜의 오후 2시 예보를 대표 날씨로 사용합니다.
 * 해당 시간대 예보가 없으면 같은 날짜에서 오후 2시와 가장 가까운 시간대 예보를 사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherForecastService {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final LocalTime REPRESENTATIVE_FORECAST_TIME = LocalTime.of(14, 0);
    private static final List<LocalTime> FORECAST_BASE_TIMES = List.of(
            LocalTime.of(2, 0),
            LocalTime.of(5, 0),
            LocalTime.of(8, 0),
            LocalTime.of(11, 0),
            LocalTime.of(14, 0),
            LocalTime.of(17, 0),
            LocalTime.of(20, 0),
            LocalTime.of(23, 0)
    );
    private static final int FORECAST_PUBLISH_DELAY_MINUTES = 10;

    private final KmaWeatherClient kmaWeatherClient;
    private final ConcurrentMap<ForecastCacheKey, Optional<WeatherForecast>> forecastCache = new ConcurrentHashMap<>();

    /**
     * 선택한 일정 날짜와 위치에 대한 대표 단기예보를 조회합니다.
     *
     * 같은 기상청 격자와 발표 시각의 요청은 캐시를 재사용하므로,
     * 여러 행사라도 동일 격자라면 외부 API를 한 번만 호출합니다.
     */
    public Optional<WeatherForecast> getRepresentativeForecast(
            LocalDate targetDate,
            double latitude,
            double longitude
    ) {
        if (!isWithinShortForecastRange(targetDate)) {
            log.info("단기예보 제공 범위를 벗어난 날짜입니다. targetDate={}", targetDate);
            return Optional.empty();
        }

        KmaGridConverter.GridCoordinate gridCoordinate = KmaGridConverter.toGrid(latitude, longitude);
        LocalDateTime baseDateTime = resolveLatestBaseDateTime();
        ForecastCacheKey cacheKey = new ForecastCacheKey(
                targetDate,
                gridCoordinate.nx(),
                gridCoordinate.ny(),
                baseDateTime
        );

        Optional<WeatherForecast> cachedForecast = forecastCache.get(cacheKey);
        if (cachedForecast != null) {
            log.info(
                    "기상청 단기예보 캐시 사용: targetDate={}, nx={}, ny={}, baseDateTime={}",
                    targetDate,
                    gridCoordinate.nx(),
                    gridCoordinate.ny(),
                    baseDateTime
            );
            return cachedForecast;
        }

        Optional<WeatherForecast> forecast = forecastCache.computeIfAbsent(
                cacheKey,
                ignored -> requestRepresentativeForecast(targetDate, gridCoordinate, baseDateTime)
        );

        log.info(
                "기상청 단기예보 조회 완료: targetDate={}, nx={}, ny={}, baseDateTime={}, found={}",
                targetDate,
                gridCoordinate.nx(),
                gridCoordinate.ny(),
                baseDateTime,
                forecast.isPresent()
        );

        return forecast;
    }

    private Optional<WeatherForecast> requestRepresentativeForecast(
            LocalDate targetDate,
            KmaGridConverter.GridCoordinate gridCoordinate,
            LocalDateTime baseDateTime
    ) {
        JsonNode response = kmaWeatherClient.getVillageForecast(
                baseDateTime.toLocalDate().toString().replace("-", ""),
                String.format("%02d00", baseDateTime.getHour()),
                gridCoordinate.nx(),
                gridCoordinate.ny()
        );

        if (!isSuccessfulResponse(response)) {
            return Optional.empty();
        }

        return extractRepresentativeForecast(response, targetDate);
    }

    private boolean isWithinShortForecastRange(LocalDate targetDate) {
        LocalDate today = LocalDate.now(KOREA_ZONE_ID);
        return !targetDate.isBefore(today) && !targetDate.isAfter(today.plusDays(2));
    }

    /**
     * 기상청 발표 시각 중 현재 시점에서 조회 가능한 가장 최근 발표 시각을 선택합니다.
     * API 반영 지연을 고려해 현재 시각보다 10분 이전에 발표된 예보만 사용합니다.
     */
    private LocalDateTime resolveLatestBaseDateTime() {
        LocalDateTime availableTime = LocalDateTime.now(KOREA_ZONE_ID)
                .minusMinutes(FORECAST_PUBLISH_DELAY_MINUTES);
        LocalDate baseDate = availableTime.toLocalDate();
        LocalTime availableLocalTime = availableTime.toLocalTime();

        for (int index = FORECAST_BASE_TIMES.size() - 1; index >= 0; index--) {
            LocalTime baseTime = FORECAST_BASE_TIMES.get(index);
            if (!baseTime.isAfter(availableLocalTime)) {
                return LocalDateTime.of(baseDate, baseTime);
            }
        }

        return LocalDateTime.of(baseDate.minusDays(1), LocalTime.of(23, 0));
    }

    private boolean isSuccessfulResponse(JsonNode response) {
        if (response == null) {
            return false;
        }

        String resultCode = response.path("response")
                .path("header")
                .path("resultCode")
                .asText();

        if (!"00".equals(resultCode)) {
            log.warn(
                    "기상청 단기예보 API가 정상 응답을 반환하지 않았습니다. resultCode={}, resultMessage={}",
                    resultCode,
                    response.path("response").path("header").path("resultMsg").asText()
            );
            return false;
        }

        return response.path("response")
                .path("body")
                .path("items")
                .path("item")
                .isArray();
    }

    private Optional<WeatherForecast> extractRepresentativeForecast(
            JsonNode response,
            LocalDate targetDate
    ) {
        String targetDateText = targetDate.toString().replace("-", "");
        Map<String, Map<String, String>> forecastsByTime = new HashMap<>();

        for (JsonNode item : response.path("response").path("body").path("items").path("item")) {
            if (!targetDateText.equals(item.path("fcstDate").asText())) {
                continue;
            }

            String forecastTime = item.path("fcstTime").asText();
            String category = item.path("category").asText();
            String forecastValue = item.path("fcstValue").asText();

            forecastsByTime
                    .computeIfAbsent(forecastTime, ignored -> new HashMap<>())
                    .put(category, forecastValue);
        }

        if (forecastsByTime.isEmpty()) {
            log.info("선택한 날짜의 단기예보가 없습니다. targetDate={}", targetDate);
            return Optional.empty();
        }

        String representativeTime = selectRepresentativeTime(forecastsByTime.keySet());
        Map<String, String> forecastValues = forecastsByTime.get(representativeTime);

        return Optional.of(new WeatherForecast(
                targetDate,
                parseForecastTime(representativeTime),
                parseInt(forecastValues.get("PTY")),
                parseInt(forecastValues.get("POP")),
                parseInt(forecastValues.get("SKY")),
                parseDouble(forecastValues.get("TMP")),
                parseDouble(forecastValues.get("WSD"))
        ));
    }

    private String selectRepresentativeTime(Iterable<String> forecastTimes) {
        List<String> times = new ArrayList<>();
        forecastTimes.forEach(times::add);

        return times.stream()
                .min(Comparator.comparingInt(time -> Math.abs(
                        parseForecastTime(time).toSecondOfDay()
                                - REPRESENTATIVE_FORECAST_TIME.toSecondOfDay()
                )))
                .orElseThrow();
    }

    private LocalTime parseForecastTime(String forecastTime) {
        if (forecastTime == null || forecastTime.length() != 4) {
            return REPRESENTATIVE_FORECAST_TIME;
        }

        return LocalTime.of(
                Integer.parseInt(forecastTime.substring(0, 2)),
                Integer.parseInt(forecastTime.substring(2, 4))
        );
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private record ForecastCacheKey(
            LocalDate targetDate,
            int nx,
            int ny,
            LocalDateTime baseDateTime
    ) {
    }

    /**
     * 추천 계산에 필요한 단기예보 항목입니다.
     *
     * precipitationType: PTY, 강수 형태
     * precipitationProbability: POP, 강수 확률
     * skyStatus: SKY, 하늘 상태
     * temperature: TMP, 1시간 기온
     * windSpeed: WSD, 풍속
     */
    public record WeatherForecast(
            LocalDate forecastDate,
            LocalTime forecastTime,
            int precipitationType,
            int precipitationProbability,
            int skyStatus,
            double temperature,
            double windSpeed
    ) {
    }
}
