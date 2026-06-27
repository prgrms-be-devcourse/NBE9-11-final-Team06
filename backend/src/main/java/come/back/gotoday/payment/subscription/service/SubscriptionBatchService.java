package come.back.gotoday.payment.subscription.service;

import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.history.entity.PaymentHistory;
import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.history.repository.PaymentHistoryRepository;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SubscriptionBatchService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    /**
     * 비관적 락을 획득한 상태에서 유예 만료 체크, 더블 체킹, pending 상태 전환을 원자적으로 수행합니다.
     */
    @Transactional
    public BatchPaymentParameters lockAndPreparePayment(Long subscriptionId, LocalDate today, List<SubscriptionStatus> targetStatuses, int graceDays) {

        // 1. Row에 비관적 락을 먼저 획득합니다.
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 2. 유예 기간 만료 체크
        if (subscription.getStatus() == SubscriptionStatus.EXPIRED_PAYMENT_PENDING
                && subscription.isGracePeriodExpired(today, graceDays)) {

            subscription.cancel(); // CANCELED로 변환

            PaymentHistory finalFailureHistory = PaymentHistory.createFailureHistory(
                    subscription,
                    "ORD-FINAL-FAIL-" + subscriptionId,
                    subscription.getAmount(),
                    "유예 기간 초과로 인한 정기 구독 강제 해지"
            );
            paymentHistoryRepository.save(finalFailureHistory);

            return BatchPaymentParameters.builder().isExpired(true).build();
        }

        // 3. 상태와 날짜를 재검증합니다. (Double-Check)
        if (!targetStatuses.contains(subscription.getStatus()) || subscription.getNextBillingDate().isAfter(today)) {
            log.info("[배치 동시성 차단] 구독 ID: {}는 이미 다른 스레드에 의해 처리되었습니다.", subscriptionId);
            return null;
        }

        // 4. ★동시성 방어 핵심: 외부 API를 찌르기 전에 상태를 "pending(결제 중)"으로 먼저 바꾸고 커밋합니다.
        SubscriptionStatus originalStatus = subscription.getStatus();
        subscription.changeStatus();
        subscriptionRepository.saveAndFlush(subscription);

        // 5. 검증 및 상태 전환에 통과했다면 결제 파라미터 반환
        return BatchPaymentParameters.builder()
                .customerKey(subscription.getBillingInfo().getCustomerKey())
                .plainBillingKey(subscription.getBillingInfo().getBillingKey())
                .snapshotAmount(subscription.getAmount())
                .planName(subscription.getPlan().getDisplayName())
                .nextBillingDate(subscription.getNextBillingDate())
                .originalStatus(originalStatus) // 원래 상태 백업
                .isExpired(false)
                .customerEmail(subscription.getBillingInfo().getMember().getEmail())
                .customerName(subscription.getBillingInfo().getMember().getNickname())
                .build();
    }

    @Transactional
    public void handleBatchPaymentFailure(Long subscriptionId, String orderId, Long amount, String failureReason, LocalDate today, SubscriptionStatus originalStatus) {
        subscriptionRepository.findById(subscriptionId).ifPresent(subscription -> {

            // 결제 시도 전 원래 유예(EXPIRED_PAYMENT_PENDING) 상태였거나, 이번이 첫 실패라면 실패 상태로 전환
            subscription.changeToPaymentBatchFail(today);


            String safeFailureReason = failureReason;
            if (safeFailureReason != null && safeFailureReason.length() > 255) {
                safeFailureReason = safeFailureReason.substring(0, 252) + "...";
            }

            PaymentHistory failureHistory = PaymentHistory.createFailureHistory(subscription, orderId, amount, safeFailureReason);
            paymentHistoryRepository.save(failureHistory);
        });
    }

    @Transactional
    public void completeScheduledPayment(Long subscriptionId, String orderId, TossAutomatedPaymentResponse tossResponse) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 1. 다음 결제 예정일 한 달 연장 및 상태를 완전히 ACTIVE(정상)로 세팅 (유예 상태였던 유저도 정상으로 복구)
        subscription.activate();

        // 2. 결제 성공 이력 영속화
        PaymentHistory successHistory = PaymentHistory.createSuccessHistory(
                subscription,
                tossResponse.paymentKey(),
                orderId,
                tossResponse.totalAmount(),
                tossResponse.getReceiptUrl()
        );
        paymentHistoryRepository.save(successHistory);
    }

    @Transactional
    public void finalizeSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (subscription.getStatus() != SubscriptionStatus.CANCELED_RESERVED) {
            log.warn("[배치] 구독 ID: {}는 해지 예약 상태가 아니므로 최종 해지를 건너뜁니다.", subscriptionId);
            return;
        }

        subscription.cancel();
        log.info("[배치 서비스] 구독 ID: {} 최종 해지 완료.", subscriptionId);
    }

    @Getter
    @Builder
    public static class BatchPaymentParameters {
        private final String customerKey;
        private final String plainBillingKey;
        private final Long snapshotAmount;
        private final String planName;
        private final LocalDate nextBillingDate;
        private final SubscriptionStatus originalStatus;
        private final boolean isExpired;

        private final String customerEmail;
        private final String customerName;
    }
}
