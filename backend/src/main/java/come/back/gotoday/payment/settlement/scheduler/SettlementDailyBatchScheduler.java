package come.back.gotoday.payment.settlement.scheduler;

import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.external.toss.dto.SettlementDto;
import come.back.gotoday.payment.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementDailyBatchScheduler {

    private final SettlementService settlementService;
    private final TossPaymentsClient tossPaymentsClient; // 외부 API 호출 모듈

    /**
     * 주정산/월정산 주기와 상관없이 '매일 새벽 3시'에 작동합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runDailySettlementJob() {
        log.info("[정산 배치 자동 실행] 장부 수집 및 대조를 시작합니다.");

        try {
            LocalDate today = LocalDate.now();
            // 계약 주기가 주정산/월정산이거나 누락/공휴일 연기를 대비해 안전하게 최근 7일간의 정산지급일을 조회 범위로 지정
            LocalDate startDate = today.minusDays(40);
            LocalDate endDate = today.minusDays(1);

            log.info("[정산 배치] 토스 API 조회 범위: {} ~ {}", startDate, endDate);

            // 토스 API 호출 (주정산 지급일이 이 범위에 걸치면 수천 건의 데이터가 올 것이고, 지급일이 아니면 빈 배열이 옴)
            List<SettlementDto.TossSettlementResponse> tossSettlements = tossPaymentsClient.fetchSettlements(startDate, endDate);

            if (tossSettlements.isEmpty()) {
                log.info("[정산 배치] 해당 기간에 토스로부터 입금(정산)된 내역이 없습니다. 배치를 종료합니다.");
                return;
            }

            // 데이터가 존재할 때만 대조 엔진 작동
            log.info("[정산 배치] 총 {}건의 정산 내역 장부 대조 시작", tossSettlements.size());
            settlementService.reconcileSettlement(tossSettlements);

            log.info("[정산 배치 완료] 정상 종료되었습니다.");

        } catch (Exception e) {
            log.error("[정산 배치 치명적 오류 파트] 에러 발생: {}", e.getMessage(), e);
        }
    }
}