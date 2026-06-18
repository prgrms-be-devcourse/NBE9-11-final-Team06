package come.back.gotoday.crowd.scheduler;

import come.back.gotoday.crowd.service.CrowdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 서울시 주요 지역의 실시간 혼잡도 데이터를 정기적으로 수집해
 * 과거 혼잡도 이력으로 DB에 누적 저장하는 스케줄러입니다.
 *
 * 다중 인스턴스 환경에서는 한 인스턴스에만
 * {@code crowd.scheduler.enabled=true}를 설정하고,
 * 나머지 인스턴스에는 {@code false}를 설정하여 중복 실행을 방지합니다.
 */
@ConditionalOnProperty(
        prefix = "crowd.scheduler",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Slf4j
@Component
@RequiredArgsConstructor
public class CrowdHistoryScheduler {

    private final CrowdService crowdService;

    /**
     * 매시 정각에 서울시에서 제공하는 전체 지역의 최신 혼잡도 데이터를 수집합니다.
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void collectCrowdHistory() {
        log.info("전체 지역 혼잡도 이력 정기 수집 시작");

        CrowdService.CrowdCollectionResult result = crowdService.refreshAllCrowdStatuses();

        log.info(
                "전체 지역 혼잡도 이력 정기 수집 완료: successCount={}, failureCount={}",
                result.successCount(),
                result.failureCount()
        );
    }
}
