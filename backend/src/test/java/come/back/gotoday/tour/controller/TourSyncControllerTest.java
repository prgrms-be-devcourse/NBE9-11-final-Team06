package come.back.gotoday.tour.controller;

import come.back.gotoday.tour.service.TourSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TourSyncControllerTest {

    @Mock
    private TourSyncService tourSyncService;

    @InjectMocks
    private TourSyncController tourSyncController;

    @Test
    @DisplayName("서울 전체 관광지 동기화 작업을 시작한다")
    void syncAllSeoulTours_success() {
        // when
        ResponseEntity<Map<String, Object>> response =
                tourSyncController.syncAllSeoulTours();

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(202);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message"))
                .isEqualTo("서울 관광지 동기화 작업이 시작되었습니다.");

        verify(tourSyncService).syncAllSeoulToursAsync();
    }

    @Test
    @DisplayName("지역 코드와 시군구 코드로 관광지 동기화 작업을 시작한다")
    void syncToursByArea_success() {
        // given
        String areaCode = "1";
        String sigunguCode = "23";

        // when
        ResponseEntity<Map<String, Object>> response =
                tourSyncController.syncToursByArea(areaCode, sigunguCode);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(202);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message"))
                .isEqualTo("관광지 동기화 작업이 시작되었습니다.");
        assertThat(body.get("areaCode")).isEqualTo(areaCode);
        assertThat(body.get("sigunguCode")).isEqualTo(sigunguCode);

        verify(tourSyncService).syncToursAsync(areaCode, sigunguCode);
    }
}