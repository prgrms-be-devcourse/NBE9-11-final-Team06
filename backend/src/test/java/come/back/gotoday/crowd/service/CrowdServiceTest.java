package come.back.gotoday.crowd.service;

import come.back.gotoday.crowd.dto.CrowdResponse;
import come.back.gotoday.crowd.entity.CongestionLevel;
import come.back.gotoday.crowd.entity.CrowdStatus;
import come.back.gotoday.crowd.repository.CrowdStatusRepository;
import come.back.gotoday.external.seoul.SeoulCrowdClient;
import come.back.gotoday.external.seoul.SeoulCrowdResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("혼잡도 조회 서비스 테스트")
class CrowdServiceTest {

    @Mock
    private SeoulCrowdClient seoulCrowdClient;

    @Mock
    private CrowdStatusRepository crowdStatusRepository;

    @InjectMocks
    private CrowdService crowdService;


    @Test
    @DisplayName("서울시 API 응답이 null이면 지역 조회 실패 예외가 발생한다")
    void getCrowdStatusThrowsWhenResponseIsNull() {
        String areaName = "강남역";
        given(crowdStatusRepository.findTopByAreaNameOrderByCreatedAtDesc(areaName))
                .willReturn(Optional.empty());
        given(seoulCrowdClient.getCrowdStatus(areaName)).willReturn(null);

        assertThatThrownBy(() -> crowdService.getCrowdStatus(areaName))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CROWD_AREA_NOT_FOUND.getMessage());

        verify(crowdStatusRepository)
                .findTopByAreaNameOrderByCreatedAtDesc(areaName);
        verify(seoulCrowdClient).getCrowdStatus(areaName);
        verify(crowdStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("CITYDATA가 없으면 지역 조회 실패 예외가 발생한다")
    void getCrowdStatusThrowsWhenCityDataIsMissing() {
        String areaName = "강남역";
        given(crowdStatusRepository.findTopByAreaNameOrderByCreatedAtDesc(areaName))
                .willReturn(Optional.empty());
        SeoulCrowdResponse response = mock(SeoulCrowdResponse.class);
        given(response.CITYDATA()).willReturn(null);
        given(seoulCrowdClient.getCrowdStatus(areaName)).willReturn(response);

        assertThatThrownBy(() -> crowdService.getCrowdStatus(areaName))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CROWD_AREA_NOT_FOUND.getMessage());

        verify(crowdStatusRepository)
                .findTopByAreaNameOrderByCreatedAtDesc(areaName);
        verify(seoulCrowdClient).getCrowdStatus(areaName);
        verify(crowdStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("실시간 인구 데이터가 없으면 혼잡도 데이터 조회 실패 예외가 발생한다")
    void getCrowdStatusThrowsWhenPopulationDataIsEmpty() {
        String areaName = "강남역";
        given(crowdStatusRepository.findTopByAreaNameOrderByCreatedAtDesc(areaName))
                .willReturn(Optional.empty());
        SeoulCrowdResponse response = mock(SeoulCrowdResponse.class);
        SeoulCrowdResponse.CityData cityData = mock(SeoulCrowdResponse.CityData.class);

        given(response.CITYDATA()).willReturn(cityData);
        given(cityData.LIVE_PPLTN_STTS()).willReturn(Collections.emptyList());
        given(seoulCrowdClient.getCrowdStatus(areaName)).willReturn(response);

        assertThatThrownBy(() -> crowdService.getCrowdStatus(areaName))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CROWD_DATA_NOT_FOUND.getMessage());

        verify(crowdStatusRepository)
                .findTopByAreaNameOrderByCreatedAtDesc(areaName);
        verify(seoulCrowdClient).getCrowdStatus(areaName);
        verify(crowdStatusRepository, never()).save(any());
    }
    @Test
    @DisplayName("서울시 API 정상 응답을 혼잡도 응답으로 변환하고 저장한다")
    void getCrowdStatusReturnsResponseAndSavesStatus() {
        String areaName = "강남역";
        given(crowdStatusRepository.findTopByAreaNameOrderByCreatedAtDesc(areaName))
                .willReturn(Optional.empty());
        SeoulCrowdResponse response = mock(SeoulCrowdResponse.class, RETURNS_DEEP_STUBS);

        given(seoulCrowdClient.getCrowdStatus(areaName)).willReturn(response);
        given(response.CITYDATA().AREA_NM()).willReturn(areaName);
        given(response.CITYDATA().AREA_CD()).willReturn("POI001");
        given(response.CITYDATA().LIVE_PPLTN_STTS().isEmpty()).willReturn(false);
        given(response.CITYDATA().LIVE_PPLTN_STTS().get(0).AREA_CONGEST_LVL())
                .willReturn("여유");
        given(response.CITYDATA().LIVE_PPLTN_STTS().get(0).AREA_CONGEST_MSG())
                .willReturn("사람이 적어 이동이 편리합니다.");
        given(response.CITYDATA().LIVE_PPLTN_STTS().get(0).AREA_PPLTN_MIN())
                .willReturn("1000");
        given(response.CITYDATA().LIVE_PPLTN_STTS().get(0).AREA_PPLTN_MAX())
                .willReturn("2000");
        given(response.CITYDATA().LIVE_PPLTN_STTS().get(0).PPLTN_TIME())
                .willReturn("2026-06-10 15:10");
        given(crowdStatusRepository.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        CrowdResponse result = crowdService.getCrowdStatus(areaName);

        assertThat(result).isNotNull();
        assertThat(result.areaName()).isEqualTo(areaName);
        verify(crowdStatusRepository)
                .findTopByAreaNameOrderByCreatedAtDesc(areaName);
        verify(seoulCrowdClient).getCrowdStatus(areaName);
        verify(crowdStatusRepository).save(any());
    }


    @Test
    @DisplayName("유효한 캐시 데이터가 존재하면 외부 API를 호출하지 않고 캐시를 반환한다")
    void getCrowdStatusReturnsCachedValueWhenFresh() {
        String areaName = "강남역";
        LocalDateTime measuredAt = LocalDateTime.now().minusMinutes(1);
        CrowdStatus cachedStatus = mock(CrowdStatus.class);

        given(cachedStatus.getCreatedAt()).willReturn(LocalDateTime.now());
        given(cachedStatus.getAreaName()).willReturn(areaName);
        given(cachedStatus.getAreaCode()).willReturn("POI001");
        given(cachedStatus.getCongestionLevel()).willReturn(CongestionLevel.RELAXED);
        given(cachedStatus.getMessage()).willReturn("여유롭습니다.");
        given(cachedStatus.getPopulationMin()).willReturn(1000);
        given(cachedStatus.getPopulationMax()).willReturn(2000);
        given(cachedStatus.getMeasuredAt()).willReturn(measuredAt);
        given(crowdStatusRepository.findTopByAreaNameOrderByCreatedAtDesc(areaName))
                .willReturn(Optional.of(cachedStatus));

        CrowdResponse result = crowdService.getCrowdStatus(areaName);

        assertThat(result).isNotNull();
        assertThat(result.areaName()).isEqualTo(areaName);
        assertThat(result.congestionLevel()).isEqualTo(CongestionLevel.RELAXED);
        assertThat(result.populationMin()).isEqualTo(1000);
        assertThat(result.populationMax()).isEqualTo(2000);
        assertThat(result.measuredAt()).isEqualTo(measuredAt);

        verify(seoulCrowdClient, never()).getCrowdStatus(any());
        verify(crowdStatusRepository, never()).save(any());
    }
    @Test
    @DisplayName("미래 혼잡도는 최근 이력 중 동일 요일과 동일 시간대 데이터의 평균으로 계산한다")
    void getPredictedCrowdStatusCalculatesAverageFromSameDayAndHourHistories() {
        String areaName = "강남역";
        LocalDateTime visitAt = LocalDateTime.of(2026, 6, 22, 15, 0);

        CrowdStatus firstHistory = mock(CrowdStatus.class);
        CrowdStatus secondHistory = mock(CrowdStatus.class);
        CrowdStatus thirdHistory = mock(CrowdStatus.class);
        CrowdStatus differentHourHistory = mock(CrowdStatus.class);
        CrowdStatus differentDayHistory = mock(CrowdStatus.class);

        given(firstHistory.getMeasuredAt()).willReturn(visitAt.minusWeeks(3));
        given(firstHistory.getPopulationMin()).willReturn(1000);
        given(firstHistory.getPopulationMax()).willReturn(2000);
        given(firstHistory.getCongestionLevel()).willReturn(CongestionLevel.RELAXED);

        given(secondHistory.getMeasuredAt()).willReturn(visitAt.minusWeeks(2));
        lenient().when(secondHistory.getAreaCode()).thenReturn("POI001");
        given(secondHistory.getPopulationMin()).willReturn(2000);
        given(secondHistory.getPopulationMax()).willReturn(3000);
        given(secondHistory.getCongestionLevel()).willReturn(CongestionLevel.RELAXED);

        given(thirdHistory.getMeasuredAt()).willReturn(visitAt.minusWeeks(1));
        lenient().when(thirdHistory.getAreaCode()).thenReturn("POI001");
        given(thirdHistory.getPopulationMin()).willReturn(3000);
        given(thirdHistory.getPopulationMax()).willReturn(4000);
        given(thirdHistory.getCongestionLevel()).willReturn(CongestionLevel.CROWDED);

        lenient().when(differentHourHistory.getMeasuredAt())
                .thenReturn(visitAt.minusWeeks(1).withHour(14));
        lenient().when(differentDayHistory.getMeasuredAt())
                .thenReturn(visitAt.minusDays(1));

        given(crowdStatusRepository
                .findAllByAreaNameAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        eq(areaName),
                        any(LocalDateTime.class),
                        eq(visitAt)
                ))
                .willReturn(List.of(
                        firstHistory,
                        secondHistory,
                        thirdHistory,
                        differentHourHistory,
                        differentDayHistory
                ));

        CrowdResponse result = crowdService.getPredictedCrowdStatus(areaName, visitAt);

        assertThat(result.areaName()).isEqualTo(areaName);
        assertThat(result.areaCode()).isEqualTo("POI001");
        assertThat(result.congestionLevel()).isEqualTo(CongestionLevel.RELAXED);
        assertThat(result.populationMin()).isEqualTo(2000);
        assertThat(result.populationMax()).isEqualTo(3000);
        assertThat(result.measuredAt()).isEqualTo(visitAt);
        assertThat(result.message()).contains("과거 동일 요일·시간대");

        verify(crowdStatusRepository)
                .findAllByAreaNameAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        eq(areaName),
                        eq(visitAt.minusWeeks(8)),
                        eq(visitAt)
                );
        verify(seoulCrowdClient, never()).getCrowdStatus(any());
    }

    @Test
    @DisplayName("예측에 사용할 과거 데이터가 없으면 현재 혼잡도 캐시를 반환한다")
    void getPredictedCrowdStatusReturnsCurrentCacheWhenHistoryIsEmpty() {
        String areaName = "강남역";
        LocalDateTime visitAt = LocalDateTime.of(2026, 6, 22, 15, 0);
        LocalDateTime measuredAt = LocalDateTime.now().minusMinutes(1);
        CrowdStatus cachedStatus = mock(CrowdStatus.class);

        given(crowdStatusRepository
                .findAllByAreaNameAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        eq(areaName),
                        any(LocalDateTime.class),
                        eq(visitAt)
                ))
                .willReturn(Collections.emptyList());
        given(crowdStatusRepository.findTopByAreaNameOrderByCreatedAtDesc(areaName))
                .willReturn(Optional.of(cachedStatus));
        given(cachedStatus.getCreatedAt()).willReturn(LocalDateTime.now());
        given(cachedStatus.getAreaName()).willReturn(areaName);
        given(cachedStatus.getAreaCode()).willReturn("POI001");
        given(cachedStatus.getCongestionLevel()).willReturn(CongestionLevel.NORMAL);
        given(cachedStatus.getMessage()).willReturn("보통입니다.");
        given(cachedStatus.getPopulationMin()).willReturn(4000);
        given(cachedStatus.getPopulationMax()).willReturn(5000);
        given(cachedStatus.getMeasuredAt()).willReturn(measuredAt);

        CrowdResponse result = crowdService.getPredictedCrowdStatus(areaName, visitAt);

        assertThat(result.areaName()).isEqualTo(areaName);
        assertThat(result.congestionLevel()).isEqualTo(CongestionLevel.NORMAL);
        assertThat(result.populationMin()).isEqualTo(4000);
        assertThat(result.populationMax()).isEqualTo(5000);
        assertThat(result.measuredAt()).isEqualTo(measuredAt);

        verify(crowdStatusRepository)
                .findTopByAreaNameOrderByCreatedAtDesc(areaName);
        verify(seoulCrowdClient, never()).getCrowdStatus(any());
        verify(crowdStatusRepository, never()).save(any());
    }
    @Test
    @DisplayName("전체 지역 수집 중 일부 지역이 실패해도 나머지 지역 수집을 계속한다")
    void refreshAllCrowdStatusesContinuesWhenOneAreaFails() {
        List<String> areaNames = List.of("강남역", "홍대 관광특구", "성수카페거리");
        CrowdService spyCrowdService = spy(crowdService);

        given(seoulCrowdClient.getAllAreaNames()).willReturn(areaNames);

        CrowdResponse gangnamResponse = mock(CrowdResponse.class);
        CrowdResponse seongsuResponse = mock(CrowdResponse.class);

        doReturn(gangnamResponse)
                .when(spyCrowdService)
                .refreshCrowdStatus("강남역");
        doThrow(new BusinessException(ErrorCode.CROWD_DATA_NOT_FOUND))
                .when(spyCrowdService)
                .refreshCrowdStatus("홍대 관광특구");
        doReturn(seongsuResponse)
                .when(spyCrowdService)
                .refreshCrowdStatus("성수카페거리");

        CrowdService.CrowdCollectionResult result =
                spyCrowdService.refreshAllCrowdStatuses();

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(1);

        verify(spyCrowdService).refreshCrowdStatus("강남역");
        verify(spyCrowdService).refreshCrowdStatus("홍대 관광특구");
        verify(spyCrowdService).refreshCrowdStatus("성수카페거리");
    }
}
