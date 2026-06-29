package come.back.gotoday.admin.controller;

import come.back.gotoday.admin.service.AdminCrowdRefreshService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCrowdControllerTest {

    @InjectMocks
    private AdminCrowdController adminCrowdController;

    @Mock
    private AdminCrowdRefreshService adminCrowdRefreshService;

    @Test
    @DisplayName("혼잡도 전체 갱신 요청이 시작되면 202 Accepted를 반환한다")
    void refreshAllCrowdStatuses_success() {
        when(adminCrowdRefreshService.startRefresh())
                .thenReturn(true);

        ResponseEntity<Void> response = adminCrowdController.refreshAllCrowdStatuses();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @DisplayName("이미 혼잡도 전체 갱신 중이면 409 Conflict를 반환한다")
    void refreshAllCrowdStatuses_alreadyRunning_fail() {
        when(adminCrowdRefreshService.startRefresh())
                .thenReturn(false);

        ResponseEntity<Void> response = adminCrowdController.refreshAllCrowdStatuses();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}