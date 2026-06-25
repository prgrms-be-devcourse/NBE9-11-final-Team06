package come.back.gotoday.payment.subscription.scheduler;

import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import come.back.gotoday.payment.subscription.service.SubscriptionBatchFacade;
import come.back.gotoday.payment.subscription.service.SubscriptionFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionBatchScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionBatchFacade subscriptionBatchFacade;

    private static final int CHUNK_SIZE = 100; // 한 번에 메모리에 올릴 청크 단위

    @Scheduled(cron = "0 5 0 * * ?") // 매일 새벽 00:05 실행
    public void runAutomatedBillingPayment() {
        LocalDate today = LocalDate.now();
        log.info("[정기 정산 배치] 자동 결제 배치를 시작합니다. 기준일자: {}", today);

        Long lastSubId = 0L;
        boolean hasNext = true;

        // 대상 상태: 활성 상태(ACTIVE) + 결제 미수 유예 상태(EXPIRED_PAYMENT_PENDING)
        List<SubscriptionStatus> targetStatuses = List.of(
                SubscriptionStatus.ACTIVE,
                SubscriptionStatus.EXPIRED_PAYMENT_PENDING,
                SubscriptionStatus.CANCELED_RESERVED
        );

        PageRequest pageRequest = PageRequest.of(0, CHUNK_SIZE, Sort.by("id").ascending());
        while (hasNext) {
            // ID 오름차순으로 정렬하여 누락 없이 페이징 처리
            Slice<Subscription> targetSlice = subscriptionRepository.findBillingTargets(lastSubId, today, targetStatuses, pageRequest);
            log.info("[정기 정산 배치] 현재 페이지: , 조회된 대상 건수: {}건", targetSlice.getNumberOfElements());
            if (targetSlice.isEmpty()) {
                break;
            }
            for (Subscription subscription : targetSlice.getContent()) {
                try {
                    if (subscription.getStatus() == SubscriptionStatus.CANCELED_RESERVED) {
                        // 결제일이 지났는지 확인
                        if (subscription.getNextBillingDate().isBefore(today.plusDays(1))) {
                            // 최종적으로 영구 해지 처리 (forceCancel 호출)
                            subscriptionBatchFacade.finalizeSubscription(subscription.getId());
                            log.info("[정기 정산 배치] 해지 예약된 구독 ID: {}가 최종 해지되었습니다.", subscription.getId());
                        }
                    } else {
                        // ACTIVE 또는 EXPIRED_PAYMENT_PENDING 인 경우에만 결제 시도
                        subscriptionBatchFacade.executeScheduledPayment(subscription.getId());
                    }
                } catch (Exception e) {
                    // 한 건이 실패하더라도 로그만 남기고 다음 사람 결제는 계속 진행되어야 함
                    log.error("[정기 정산 배치] 구독 ID: {} 결제 승인 중 예외 발생. 다음 건으로 넘어갑니다. 사유: {}",
                            subscription.getId(), e.getMessage());
                }
                lastSubId = subscription.getId();
            }

            hasNext = targetSlice.hasNext();
        }

        log.info("[정기 정산 배치] 자동 결제 배치가 완전히 종료되었습니다.");
    }
}