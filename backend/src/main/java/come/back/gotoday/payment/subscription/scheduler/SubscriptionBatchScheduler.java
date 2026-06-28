package come.back.gotoday.payment.subscription.scheduler;

import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import come.back.gotoday.payment.subscription.service.SubscriptionBatchFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionBatchScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionBatchFacade subscriptionBatchFacade;
    private final ApplicationEventPublisher eventPublisher;

    // 비동기 스레드 풀 주입
    private final TaskExecutor schedulerTaskExecutor;

    private static final int CHUNK_SIZE = 100; // 한 번에 메모리에 올릴 청크 단위

    @Scheduled(cron = "0 5 0 * * ?") // 매일 새벽 00:05 실행
    public void runAutomatedBillingPayment() {
        LocalDate today = LocalDate.now();
        log.info("[정기 정산 배치] 자동 결제 배치를 시작합니다. 기준일자: {}", today);

        Long lastSubId = 0L;
        boolean hasNext = true;

        // 대상 상태: 활성 상태(ACTIVE) + 결제 미수 유예 상태(EXPIRED_PAYMENT_PENDING) + 해지 예약 상태(CANCELED_RESERVED)
        List<SubscriptionStatus> targetStatuses = List.of(
                SubscriptionStatus.ACTIVE,
                SubscriptionStatus.EXPIRED_PAYMENT_PENDING,
                SubscriptionStatus.CANCELED_RESERVED
        );

        PageRequest pageRequest = PageRequest.of(0, CHUNK_SIZE, Sort.by("id").ascending());
        while (hasNext) {
            // ID 오름차순으로 정렬하여 누락 없이 페이징 처리
            Slice<Subscription> targetSlice = subscriptionRepository.findBillingTargets(lastSubId, today, targetStatuses, pageRequest);
            log.info("[정기 정산 배치] 현재 페이지 조회된 대상 건수: {}건", targetSlice.getNumberOfElements());

            if (targetSlice.isEmpty()) {
                break;
            }

            // 1. 이번 청크(100건)의 비동기 작업 리스트 생성
            List<CompletableFuture<Void>> futures = targetSlice.getContent().stream()
                    .map(subscription -> CompletableFuture.runAsync(() -> {
                        try {
                            if (subscription.getStatus() == SubscriptionStatus.CANCELED_RESERVED) {
                                // 결제일이 지났는지 확인
                                if (subscription.getNextBillingDate().isBefore(today.plusDays(1))) {
                                    // 최종적으로 영구 해지 처리
                                    subscriptionBatchFacade.finalizeSubscription(subscription.getId());
                                    log.info("[정기 정산 배치] 해지 예약된 구독 ID: {}가 최종 해지되었습니다. 스레드: {}",
                                            subscription.getId(), Thread.currentThread().getName());
                                }
                            } else {
                                // ACTIVE 또는 EXPIRED_PAYMENT_PENDING 인 경우 결제 시도
                                subscriptionBatchFacade.executeScheduledPayment(subscription.getId());
                                log.info("[정기 정산 배치] 구독 ID: {} 결제 승인 완료. 스레드: {}",
                                        subscription.getId(), Thread.currentThread().getName());
                            }
                        } catch (Exception e) {
                            // 한 건이 실패하더라도 로그만 남기고 다음 비동기 스레드 작업들은 계속 진행됨
                            log.error("[정기 정산 배치] 구독 ID: {} 처리 중 예외 발생. 다음 건으로 넘어갑니다. 사유: {}",
                                    subscription.getId(), e.getMessage());
                        }
                    }, schedulerTaskExecutor))
                    .toList();

            // 2. 이번 청크의 모든 비동기 작업이 끝날 때까지 대기(Join)하여 DB 커넥션 및 가용 자원 안정성 확보
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 3. 기존 코드 흐름을 손상하지 않고, 이번 청크의 가장 마지막 데이터 ID로 안전하게 lastSubId 갱신
            lastSubId = targetSlice.getContent().get(targetSlice.getNumberOfElements() - 1).getId();
            hasNext = targetSlice.hasNext();
        }

        log.info("[정기 정산 배치] 자동 결제 배치가 완전히 종료되었습니다.");
        eventPublisher.publishEvent(new SubscriptionBatchCompleteEvent(this, today));
    }

    public class SubscriptionBatchCompleteEvent extends ApplicationEvent {
        private final LocalDate batchDate;
        public SubscriptionBatchCompleteEvent(Object source, LocalDate batchDate) {
            super(source);
            this.batchDate = batchDate;
        }
        public LocalDate getBatchDate() { return batchDate; }
    }
}