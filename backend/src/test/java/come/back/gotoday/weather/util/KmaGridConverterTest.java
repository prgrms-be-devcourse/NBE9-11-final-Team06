package come.back.gotoday.weather.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("기상청 격자 좌표 변환기 단위 테스트")
class KmaGridConverterTest {

    @Test
    @DisplayName("서울시청 좌표를 기상청 격자 좌표로 변환한다")
    void toGridConvertsSeoulCityHallCoordinate() {
        KmaGridConverter.GridCoordinate result = KmaGridConverter.toGrid(
                37.5665,
                126.9780
        );

        assertThat(result.nx()).isEqualTo(60);
        assertThat(result.ny()).isEqualTo(127);
    }

    @Test
    @DisplayName("같은 좌표는 항상 같은 기상청 격자 좌표로 변환한다")
    void toGridReturnsSameGridForSameCoordinate() {
        KmaGridConverter.GridCoordinate first = KmaGridConverter.toGrid(
                37.5665,
                126.9780
        );
        KmaGridConverter.GridCoordinate second = KmaGridConverter.toGrid(
                37.5665,
                126.9780
        );

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("서로 다른 서울 지역 좌표는 유효한 양수 격자 좌표로 변환한다")
    void toGridConvertsDifferentSeoulCoordinatesToValidGrid() {
        KmaGridConverter.GridCoordinate jongno = KmaGridConverter.toGrid(
                37.5720,
                126.9794
        );
        KmaGridConverter.GridCoordinate jamsil = KmaGridConverter.toGrid(
                37.5133,
                127.1001
        );

        assertThat(jongno.nx()).isPositive();
        assertThat(jongno.ny()).isPositive();
        assertThat(jamsil.nx()).isPositive();
        assertThat(jamsil.ny()).isPositive();
        assertThat(jongno).isNotEqualTo(jamsil);
    }
}
