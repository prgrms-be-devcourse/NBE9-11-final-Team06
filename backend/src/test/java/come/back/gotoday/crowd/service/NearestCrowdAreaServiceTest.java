package come.back.gotoday.crowd.service;

import come.back.gotoday.crowd.entity.CongestionLevel;
import come.back.gotoday.crowd.entity.CrowdStatus;
import come.back.gotoday.crowd.repository.CrowdStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NearestCrowdAreaServiceTest {

    @Mock
    private CrowdStatusRepository crowdStatusRepository;

    private NearestCrowdAreaService nearestCrowdAreaService;

    @BeforeEach
    void setUp() {
        nearestCrowdAreaService = new NearestCrowdAreaService(crowdStatusRepository);
    }

    @Test
    @DisplayName("사용자 위치에서 가장 가까운 혼잡도 지역을 반환한다")
    void findNearest_returnsNearestCrowdArea() {
        // given
        CrowdStatus gangnam = createCrowdStatus(
                "강남역",
                "POI001",
                37.4979,
                127.0276
        );
        CrowdStatus yeoksam = createCrowdStatus(
                "역삼역",
                "POI002",
                37.5007,
                127.0365
        );

        given(crowdStatusRepository.findLatestByArea())
                .willReturn(List.of(gangnam, yeoksam));

        // when
        Optional<NearestCrowdAreaService.NearestCrowdArea> result =
                nearestCrowdAreaService.findNearest(37.4980, 127.0277);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().areaName()).isEqualTo("강남역");
        assertThat(result.get().areaCode()).isEqualTo("POI001");
        assertThat(result.get().latitude()).isEqualTo(37.4979);
        assertThat(result.get().longitude()).isEqualTo(127.0276);
        assertThat(result.get().distanceKm()).isLessThan(0.1);
        assertThat(result.get().congestionLevel()).isEqualTo(CongestionLevel.RELAXED);
    }

    @Test
    @DisplayName("가장 가까운 혼잡도 지역이 3km를 초과하면 빈 결과를 반환한다")
    void findNearest_overMaximumDistance_returnsEmpty() {
        // given
        CrowdStatus gangnam = createCrowdStatus(
                "강남역",
                "POI001",
                37.4979,
                127.0276
        );

        given(crowdStatusRepository.findLatestByArea())
                .willReturn(List.of(gangnam));

        // when
        Optional<NearestCrowdAreaService.NearestCrowdArea> result =
                nearestCrowdAreaService.findNearest(37.5665, 126.9780);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("혼잡도 지역 데이터가 없으면 빈 결과를 반환한다")
    void findNearest_noCrowdArea_returnsEmpty() {
        // given
        given(crowdStatusRepository.findLatestByArea())
                .willReturn(List.of());

        // when
        Optional<NearestCrowdAreaService.NearestCrowdArea> result =
                nearestCrowdAreaService.findNearest(37.4979, 127.0276);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("위도가 유효 범위를 벗어나면 예외가 발생한다")
    void findNearest_invalidLatitude_throwsException() {
        // when & then
        assertThatThrownBy(() -> nearestCrowdAreaService.findNearest(90.1, 127.0276))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("위도는 -90 이상 90 이하여야 합니다.");

        verify(crowdStatusRepository, never()).findLatestByArea();
    }

    @Test
    @DisplayName("경도가 유효 범위를 벗어나면 예외가 발생한다")
    void findNearest_invalidLongitude_throwsException() {
        // when & then
        assertThatThrownBy(() -> nearestCrowdAreaService.findNearest(37.4979, 180.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경도는 -180 이상 180 이하여야 합니다.");

        verify(crowdStatusRepository, never()).findLatestByArea();
    }

    private CrowdStatus createCrowdStatus(
            String areaName,
            String areaCode,
            double latitude,
            double longitude
    ) {
        return CrowdStatus.create(
                null,
                areaName,
                areaCode,
                latitude,
                longitude,
                CongestionLevel.RELAXED,
                1_000,
                2_000,
                "테스트 혼잡도 메시지",
                LocalDateTime.of(2026, 6, 18, 9, 0)
        );
    }
}
