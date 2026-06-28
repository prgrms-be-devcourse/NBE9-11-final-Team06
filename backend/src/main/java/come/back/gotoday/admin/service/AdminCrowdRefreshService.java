package come.back.gotoday.admin.service;

import come.back.gotoday.crowd.service.CrowdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCrowdRefreshService {

    private final CrowdService crowdService;

    @Qualifier("crowdRefreshTaskExecutor")
    private final TaskExecutor crowdRefreshTaskExecutor;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public boolean startRefresh() {
        if (!running.compareAndSet(false, true)) {
            log.warn("관리자 혼잡도 전체 갱신 요청 거부: 이미 갱신 작업이 진행 중입니다.");
            return false;
        }

        crowdRefreshTaskExecutor.execute(() -> {
            try {
                log.info("관리자 혼잡도 전체 갱신 비동기 작업 시작");

                CrowdService.CrowdCollectionResult result = crowdService.refreshAllCrowdStatuses();

                log.info(
                        "관리자 혼잡도 전체 갱신 비동기 작업 완료: successCount={}, failureCount={}",
                        result.successCount(),
                        result.failureCount()
                );
            } catch (Exception e) {
                log.error("관리자 혼잡도 전체 갱신 비동기 작업 중 오류 발생", e);
            } finally {
                running.set(false);
            }
        });

        return true;
    }
}