package come.back.gotoday.crowd.controller;

import come.back.gotoday.crowd.dto.CrowdResponse;
import come.back.gotoday.crowd.service.CrowdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("혼잡도 조회 컨트롤러 테스트")
class CrowdControllerTest {

    @Mock
    private CrowdService crowdService;

    private MockMvc mockMvc;
    private CrowdController crowdController;

    @BeforeEach
    void setUp() {
        crowdController = new CrowdController(crowdService);
        mockMvc = MockMvcBuilders.standaloneSetup(crowdController).build();
    }

    @Test
    @DisplayName("정상 지역명을 전달하면 혼잡도 조회에 성공한다")
    void getCrowdStatusSucceedsWithValidAreaName() throws Exception {
        String areaName = "성수카페거리";
        CrowdResponse response = mock(CrowdResponse.class);
        given(crowdService.getCrowdStatus(areaName)).willReturn(response);

        mockMvc.perform(get("/api/crowds")
                        .param("areaName", areaName))
                .andExpect(status().isOk());

        verify(crowdService).getCrowdStatus(areaName);
    }

    @Test
    @DisplayName("지역명과 좌표가 모두 없으면 예외가 발생한다")
    void getCrowdStatusThrowsExceptionWhenNoLookupConditionIsProvided() {
        assertThatThrownBy(() -> crowdController.getCrowdStatus(null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지역명 또는 위도·경도는 필수입니다.");
    }

    @Test
    @DisplayName("위도와 경도가 함께 전달되면 최근접 혼잡도 조회에 성공한다")
    void getCrowdStatusSucceedsWithCoordinates() throws Exception {
        double latitude = 37.5446;
        double longitude = 127.0558;
        CrowdResponse response = mock(CrowdResponse.class);
        given(crowdService.getNearestCrowdStatus(latitude, longitude)).willReturn(response);

        mockMvc.perform(get("/api/crowds")
                        .param("latitude", String.valueOf(latitude))
                        .param("longitude", String.valueOf(longitude)))
                .andExpect(status().isOk());

        verify(crowdService).getNearestCrowdStatus(latitude, longitude);
    }

    @Test
    @DisplayName("위도 또는 경도 중 하나만 전달되면 예외가 발생한다")
    void getCrowdStatusThrowsExceptionWhenOnlyOneCoordinateIsProvided() {
        assertThatThrownBy(() -> crowdController.getCrowdStatus(null, 37.5446, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("혼잡도 좌표 조회에는 위도와 경도를 함께 전달해야 합니다.");
    }
}
