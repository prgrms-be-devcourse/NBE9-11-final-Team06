package come.back.gotoday.admin.service;

import come.back.gotoday.crowd.service.CrowdService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCrowdRefreshServiceTest {

    @Mock
    private CrowdService crowdService;

    @Test
    @DisplayName("혼잡도 전체 갱신 작업을 시작할 수 있다")
    void startRefresh_success() {
        TaskExecutor directExecutor = Runnable::run;
        AdminCrowdRefreshService adminCrowdRefreshService =
                new AdminCrowdRefreshService(crowdService, directExecutor);

        when(crowdService.refreshAllCrowdStatuses())
                .thenReturn(new CrowdService.CrowdCollectionResult(10, 0));

        boolean result = adminCrowdRefreshService.startRefresh();

        assertThat(result).isTrue();
        verify(crowdService).refreshAllCrowdStatuses();
    }

    @Test
    @DisplayName("이미 혼잡도 갱신 작업이 진행 중이면 새 작업을 시작하지 않는다")
    void startRefresh_alreadyRunning_fail() {
        TaskExecutor blockingExecutor = command -> {
            // 작업을 실행하지 않아 running 상태를 유지한다.
        };
        AdminCrowdRefreshService adminCrowdRefreshService =
                new AdminCrowdRefreshService(crowdService, blockingExecutor);

        boolean firstResult = adminCrowdRefreshService.startRefresh();
        boolean secondResult = adminCrowdRefreshService.startRefresh();

        assertThat(firstResult).isTrue();
        assertThat(secondResult).isFalse();
        verifyNoInteractions(crowdService);
    }

    @Test
    @DisplayName("혼잡도 갱신 중 예외가 발생해도 다음 갱신 요청을 다시 시작할 수 있다")
    void startRefresh_exception_releaseRunningState() {
        TaskExecutor directExecutor = Runnable::run;
        AdminCrowdRefreshService adminCrowdRefreshService =
                new AdminCrowdRefreshService(crowdService, directExecutor);

        when(crowdService.refreshAllCrowdStatuses())
                .thenThrow(new RuntimeException("서울시 API 호출 실패"))
                .thenReturn(new CrowdService.CrowdCollectionResult(5, 1));

        boolean firstResult = adminCrowdRefreshService.startRefresh();
        boolean secondResult = adminCrowdRefreshService.startRefresh();

        assertThat(firstResult).isTrue();
        assertThat(secondResult).isTrue();
        verify(crowdService, times(2)).refreshAllCrowdStatuses();
    }
}