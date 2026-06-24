package come.back.gotoday.payment.idempotency.scheduler;

import come.back.gotoday.payment.idempotency.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyCleanUpScheduler {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    /**
     * 매 시간 정각마다 유효기간(1시간)이 지난 멱등성 키를 일괄 청소합니다.
     * Cron 표현식: "0 0 * * * *" (초 분 시 일 월 요일) -> 매시 0분 0초 작동
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanUpExpiredIdempotencyKeys() {
        // 현재 시간 기준으로 정확히 1시간 전의 시점을 계산합니다.
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);

        log.info("[Idempotency CleanUp] 만료된 멱등키 청소 스케줄러 가동 시작. 기준 시간(1시간 전): {}", threshold);

        try {
            // 1시간보다 더 과거에 생성된(createdAt < threshold) 레코드 일괄 제거
            int deletedCount = idempotencyKeyRepository.deleteExpiredKeys(threshold);

            log.info("[Idempotency CleanUp] 청소 완료. 총 {} 건의 만료된 멱등성 키가 삭제되었습니다.", deletedCount);
        } catch (Exception e) {
            log.error("[Idempotency CleanUp] 멱등성 키 일괄 청소 중 예외 발생: ", e);
            // 결제 자체에 영향을 주면 안 되므로 예외를 가볍게 로깅하고 마무리합니다.
        }
    }
}