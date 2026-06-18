package come.back.gotoday.crowd.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoDistanceCalculatorTest {

    @Test
    @DisplayName("동일한 좌표 사이의 거리는 0km이다")
    void calculateKilometers_sameCoordinates_returnsZero() {
        // given
        double latitude = 37.4979;
        double longitude = 127.0276;

        // when
        double distance = GeoDistanceCalculator.calculateKilometers(
                latitude,
                longitude,
                latitude,
                longitude
        );

        // then
        assertThat(distance).isZero();
    }

    @Test
    @DisplayName("서로 다른 두 좌표 사이의 거리를 km 단위로 계산한다")
    void calculateKilometers_differentCoordinates_returnsDistance() {
        // given
        double gangnamLatitude = 37.4979;
        double gangnamLongitude = 127.0276;
        double seongsuLatitude = 37.5445;
        double seongsuLongitude = 127.0560;

        // when
        double distance = GeoDistanceCalculator.calculateKilometers(
                gangnamLatitude,
                gangnamLongitude,
                seongsuLatitude,
                seongsuLongitude
        );

        // then
        assertThat(distance).isBetween(5.7, 5.8);
    }

    @Test
    @DisplayName("좌표의 순서를 바꿔도 계산된 거리는 동일하다")
    void calculateKilometers_reversedCoordinates_returnsSameDistance() {
        // given
        double latitude1 = 37.4979;
        double longitude1 = 127.0276;
        double latitude2 = 37.5445;
        double longitude2 = 127.0560;

        // when
        double forwardDistance = GeoDistanceCalculator.calculateKilometers(
                latitude1,
                longitude1,
                latitude2,
                longitude2
        );
        double reverseDistance = GeoDistanceCalculator.calculateKilometers(
                latitude2,
                longitude2,
                latitude1,
                longitude1
        );

        // then
        assertThat(forwardDistance).isEqualTo(reverseDistance);
    }
}
