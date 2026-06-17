package come.back.gotoday.crowd.scheduler;

import come.back.gotoday.crowd.service.CrowdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrowdHistorySchedulerTest {

    @Mock
    private CrowdService crowdService;

    private CrowdHistoryScheduler crowdHistoryScheduler;

    @BeforeEach
    void setUp() {
        crowdHistoryScheduler = new CrowdHistoryScheduler(crowdService);
    }

    @Test
    @DisplayName("정기 수집 실행 시 전체 지역 혼잡도 갱신을 요청한다")
    void collectCrowdHistoryRefreshesAllCrowdStatuses() {
        CrowdService.CrowdCollectionResult collectionResult =
                new CrowdService.CrowdCollectionResult(120, 1);

        given(crowdService.refreshAllCrowdStatuses())
                .willReturn(collectionResult);

        crowdHistoryScheduler.collectCrowdHistory();

        verify(crowdService).refreshAllCrowdStatuses();
    }
}
