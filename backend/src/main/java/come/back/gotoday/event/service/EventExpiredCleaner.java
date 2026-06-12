package come.back.gotoday.event.service;


import come.back.gotoday.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventExpiredCleaner {

    private final EventRepository eventRepository;

    /**
     * 오늘 날짜 이전의 모든 만료된 이벤트를 일괄 삭제합니다.
     */
    @Transactional
    public void cleanExpiredEvents() {
        LocalDate today = LocalDate.now();
        log.info(" [Clean-up] 만료된 과거 행사 데이터 청소 시작... 기준일: {}", today);

        try {
            int deletedCount = eventRepository.deleteExpiredEvents(today);
            log.info("✅ [Clean-up] 청소 완료! 총 {}건의 만료된 행사가 DB에서 삭제되었습니다.", deletedCount);
        } catch (Exception e) {
            log.error("[Clean-up] 만료 데이터 청소 중 오류 발생: {}", e.getMessage(), e);
            throw e; // 배치 전체 트랜잭션 롤백을 위해 예외를 상위로 던짐
        }
    }
}