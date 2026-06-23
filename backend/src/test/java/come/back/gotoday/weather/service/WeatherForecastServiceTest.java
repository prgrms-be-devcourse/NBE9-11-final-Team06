package come.back.gotoday.weather.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import come.back.gotoday.external.weather.KmaWeatherClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("기상청 단기예보 서비스 단위 테스트")
class WeatherForecastServiceTest {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final KmaWeatherClient kmaWeatherClient = mock(KmaWeatherClient.class);
    private final WeatherForecastService weatherForecastService = new WeatherForecastService(kmaWeatherClient);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("단기예보 범위를 벗어난 날짜는 외부 API를 호출하지 않고 빈 값을 반환한다")
    void getRepresentativeForecastReturnsEmptyWhenTargetDateIsOutOfRange() {
        LocalDate outOfRangeDate = LocalDate.now(KOREA_ZONE_ID).plusDays(3);

        Optional<WeatherForecastService.WeatherForecast> result = weatherForecastService
                .getRepresentativeForecast(outOfRangeDate, 37.5665, 126.9780);

        assertThat(result).isEmpty();
        verifyNoInteractions(kmaWeatherClient);
    }

    @Test
    @DisplayName("정상 응답에서 선택 날짜의 오후 2시 예보를 대표 예보로 추출한다")
    void getRepresentativeForecastExtractsRepresentativeForecast() throws Exception {
        LocalDate targetDate = LocalDate.now(KOREA_ZONE_ID);
        when(kmaWeatherClient.getVillageForecast(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(objectMapper.readTree(successResponse(targetDate)));

        Optional<WeatherForecastService.WeatherForecast> result = weatherForecastService
                .getRepresentativeForecast(targetDate, 37.5665, 126.9780);

        assertThat(result).isPresent();
        assertThat(result.get().forecastDate()).isEqualTo(targetDate);
        assertThat(result.get().forecastTime().getHour()).isEqualTo(14);
        assertThat(result.get().precipitationType()).isZero();
        assertThat(result.get().precipitationProbability()).isEqualTo(20);
        assertThat(result.get().skyStatus()).isEqualTo(3);
        assertThat(result.get().temperature()).isEqualTo(25.0);
        assertThat(result.get().windSpeed()).isEqualTo(2.1);
    }

    @Test
    @DisplayName("같은 날짜와 같은 기상청 격자 요청은 캐시를 재사용한다")
    void getRepresentativeForecastUsesCacheForSameGrid() throws Exception {
        LocalDate targetDate = LocalDate.now(KOREA_ZONE_ID);
        when(kmaWeatherClient.getVillageForecast(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(objectMapper.readTree(successResponse(targetDate)));

        Optional<WeatherForecastService.WeatherForecast> first = weatherForecastService
                .getRepresentativeForecast(targetDate, 37.5665, 126.9780);
        Optional<WeatherForecastService.WeatherForecast> second = weatherForecastService
                .getRepresentativeForecast(targetDate, 37.5665, 126.9780);

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(second.get()).isEqualTo(first.get());
        verify(kmaWeatherClient, times(1))
                .getVillageForecast(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("기상청 API가 실패 응답을 반환하면 빈 값을 반환한다")
    void getRepresentativeForecastReturnsEmptyWhenKmaResponseFails() throws Exception {
        LocalDate targetDate = LocalDate.now(KOREA_ZONE_ID);
        when(kmaWeatherClient.getVillageForecast(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "response": {
                            "header": {
                              "resultCode": "03",
                              "resultMsg": "NO_DATA"
                            }
                          }
                        }
                        """));

        Optional<WeatherForecastService.WeatherForecast> result = weatherForecastService
                .getRepresentativeForecast(targetDate, 37.5665, 126.9780);

        assertThat(result).isEmpty();
    }

    private String successResponse(LocalDate targetDate) {
        String date = targetDate.toString().replace("-", "");

        return """
                {
                  "response": {
                    "header": {
                      "resultCode": "00",
                      "resultMsg": "NORMAL_SERVICE"
                    },
                    "body": {
                      "items": {
                        "item": [
                          {"fcstDate":"%s","fcstTime":"1400","category":"PTY","fcstValue":"0"},
                          {"fcstDate":"%s","fcstTime":"1400","category":"POP","fcstValue":"20"},
                          {"fcstDate":"%s","fcstTime":"1400","category":"SKY","fcstValue":"3"},
                          {"fcstDate":"%s","fcstTime":"1400","category":"TMP","fcstValue":"25"},
                          {"fcstDate":"%s","fcstTime":"1400","category":"WSD","fcstValue":"2.1"},
                          {"fcstDate":"%s","fcstTime":"1700","category":"TMP","fcstValue":"22"}
                        ]
                      }
                    }
                  }
                }
                """.formatted(date, date, date, date, date, date);
    }
}
