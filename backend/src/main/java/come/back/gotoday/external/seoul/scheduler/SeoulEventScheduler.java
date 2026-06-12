package come.back.gotoday.external.seoul.scheduler;

import come.back.gotoday.event.service.EventBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeoulEventScheduler {

    private final EventBatchService eventBatchService;

    /**
     * 매일 새벽 4시에 서울시 문화행사 API 데이터를 동기화합니다.
     * 크론 표현식 (Cron Expression): 초 분 시 일 월 요일
     * "0 0 4 * * *" = 매일 04시 00분 00초
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void runSeoulEventSync() {
        log.info("🕒 [스케줄러] 서울시 문화행사 배치 동기화 시작");

        try {
            eventBatchService.syncSeoulEvents();
            log.info("[스케줄러] 서울시 문화행사 배치 동기화 성공적으로 완료");
        } catch (Exception e) {
            log.error("[스케줄러] 배치 동기화 중 예외 발생: ", e);
        }
    }
}