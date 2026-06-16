package come.back.gotoday.crowd.service;

import come.back.gotoday.external.seoul.SeoulCrowdClient;
import come.back.gotoday.external.seoul.SeoulCrowdResponse;
import come.back.gotoday.crowd.repository.CrowdStatusRepository;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("혼잡도 조회 서비스 테스트")
class CrowdServiceTest {

    @Mock
    private SeoulCrowdClient seoulCrowdClient;

    @Mock
    private CrowdStatusRepository crowdStatusRepository;

    @InjectMocks
    private CrowdService crowdService;

    @BeforeEach
    void setUp() {
        given(crowdStatusRepository.findTopByAreaNameOrderByCreatedAtDesc("강남역"))
                .willReturn(Optional.empty());
    }

    @Test
    @DisplayName("서울시 API 응답이 null이면 지역 조회 실패 예외가 발생한다")
    void getCrowdStatusThrowsWhenResponseIsNull() {
        String areaName = "강남역";
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

        Object result = crowdService.getCrowdStatus(areaName);

        assertThat(result).isNotNull();
        verify(crowdStatusRepository)
                .findTopByAreaNameOrderByCreatedAtDesc(areaName);
        verify(seoulCrowdClient).getCrowdStatus(areaName);
        verify(crowdStatusRepository).save(any());
    }
}
