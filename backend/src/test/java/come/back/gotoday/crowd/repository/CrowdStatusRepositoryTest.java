package come.back.gotoday.crowd.repository;

import come.back.gotoday.crowd.entity.CongestionLevel;
import come.back.gotoday.crowd.entity.CrowdStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("혼잡도 이력 저장소 테스트")
class CrowdStatusRepositoryTest {

    @Autowired
    private CrowdStatusRepository crowdStatusRepository;

    @Test
    @DisplayName("지역명과 측정 시각 범위로 혼잡도 이력을 오래된 순서대로 조회한다")
    void findAllByAreaNameAndMeasuredAtBetweenOrderByMeasuredAtAsc() {
        String areaName = "강남역";
        LocalDateTime baseTime = LocalDateTime.of(2026, 6, 1, 15, 0);

        CrowdStatus first = createCrowdStatus(
                areaName,
                baseTime.plusWeeks(1),
                1000,
                2000,
                CongestionLevel.RELAXED
        );
        CrowdStatus second = createCrowdStatus(
                areaName,
                baseTime.plusWeeks(2),
                2000,
                3000,
                CongestionLevel.NORMAL
        );
        CrowdStatus outOfRange = createCrowdStatus(
                areaName,
                baseTime.minusDays(1),
                3000,
                4000,
                CongestionLevel.CROWDED
        );
        CrowdStatus anotherArea = createCrowdStatus(
                "홍대 관광특구",
                baseTime.plusWeeks(1),
                4000,
                5000,
                CongestionLevel.CROWDED
        );

        crowdStatusRepository.saveAll(List.of(
                second,
                outOfRange,
                anotherArea,
                first
        ));
        crowdStatusRepository.flush();

        List<CrowdStatus> result =
                crowdStatusRepository
                        .findAllByAreaNameAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                                areaName,
                                baseTime,
                                baseTime.plusWeeks(3)
                        );

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(CrowdStatus::getMeasuredAt)
                .containsExactly(
                        baseTime.plusWeeks(1),
                        baseTime.plusWeeks(2)
                );
        assertThat(result)
                .extracting(CrowdStatus::getAreaName)
                .containsOnly(areaName);
    }

    @Test
    @DisplayName("좌표가 있는 각 지역의 최신 혼잡도 데이터만 조회한다")
    void findLatestByArea() {
        // given
        LocalDateTime baseTime = LocalDateTime.of(2026, 6, 18, 9, 0);

        CrowdStatus oldGangnam = createCrowdStatus(
                "강남역",
                baseTime.minusHours(1),
                1_000,
                2_000,
                CongestionLevel.RELAXED
        );
        crowdStatusRepository.saveAndFlush(oldGangnam);

        CrowdStatus latestGangnam = createCrowdStatus(
                "강남역",
                baseTime,
                2_000,
                3_000,
                CongestionLevel.NORMAL
        );
        CrowdStatus latestHongdae = createCrowdStatus(
                "홍대 관광특구",
                baseTime,
                3_000,
                4_000,
                CongestionLevel.CROWDED
        );
        CrowdStatus noCoordinate = CrowdStatus.create(
                null,
                "좌표 없는 지역",
                "POI999",
                null,
                null,
                CongestionLevel.RELAXED,
                500,
                1_000,
                "좌표 없는 테스트 데이터",
                baseTime
        );

        crowdStatusRepository.saveAll(List.of(
                latestGangnam,
                latestHongdae,
                noCoordinate
        ));
        crowdStatusRepository.flush();

        // when
        List<CrowdStatus> result = crowdStatusRepository.findLatestByArea();

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(CrowdStatus::getAreaName)
                .containsExactlyInAnyOrder("강남역", "홍대 관광특구");
        assertThat(result)
                .filteredOn(crowdStatus -> crowdStatus.getAreaName().equals("강남역"))
                .singleElement()
                .extracting(CrowdStatus::getMeasuredAt)
                .isEqualTo(baseTime);
        assertThat(result)
                .extracting(CrowdStatus::getAreaName)
                .doesNotContain("좌표 없는 지역");
    }

    private CrowdStatus createCrowdStatus(
            String areaName,
            LocalDateTime measuredAt,
            int populationMin,
            int populationMax,
            CongestionLevel congestionLevel
    ) {
        return CrowdStatus.create(
                null,
                areaName,
                "POI001",
                37.4979,
                127.0276,
                congestionLevel,
                populationMin,
                populationMax,
                "테스트 혼잡도 메시지",
                measuredAt
        );
    }
}
