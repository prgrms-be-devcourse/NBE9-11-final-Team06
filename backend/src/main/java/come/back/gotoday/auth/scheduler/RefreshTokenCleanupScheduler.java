package come.back.gotoday.auth.scheduler;

import come.back.gotoday.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void deleteExpiredRefreshTokens() {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        log.info("만료 Refresh Token 정리 스케줄러 시작: 기준시각={}", now);

        try {
            refreshTokenRepository.deleteByExpiresAtBefore(now);
            log.info("만료 Refresh Token 정리 스케줄러 완료: 기준시각={}", now);
        } catch (RuntimeException exception) {
            log.error("만료 Refresh Token 정리 스케줄러 실패: 기준시각={}, message={}", now, exception.getMessage(), exception);
            throw exception;
        }
    }
}