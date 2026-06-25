package come.back.gotoday.payment.subscription.service;


import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.external.toss.dto.TossAutomatedPaymentRequest;
import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionBatchFacade {

    private final TossPaymentsClient tossPaymentsClient;
    private final SubscriptionBatchService subscriptionBatchService;
    private final SubscriptionRepository subscriptionRepository;
    // private final NotificationService notificationService; // 알림톡/이메일 발송 서비스 (가정)

    private static final int GRACE_PERIOD_DAYS = 7; // 유예 기간 정책: 7일

    public void executeScheduledPayment(Long subscriptionId) {
        LocalDate today = LocalDate.now();
        List<SubscriptionStatus> targetStatuses = List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.EXPIRED_PAYMENT_PENDING);

        var paymentParams = subscriptionBatchService.lockAndPreparePayment(subscriptionId, today, targetStatuses, GRACE_PERIOD_DAYS);

        // 다른 스레드가 선점했거나(null), 유예 만료로 해지된 경우(isExpired) 결제 진행 없이 즉시 안전하게 탈출합니다.
        if (paymentParams == null) {
            return;
        }
        if (paymentParams.isExpired()) {
            log.info("[배치 유예 만료] 구독 ID: {} 유예 기간 {}일 초과로 인해 강제 해지 처리 완료.", subscriptionId, GRACE_PERIOD_DAYS);
            return;
        }

        // --- 이하 유예 기간 이내이거나 정상 결제 대상인 경우 기존 로직 수행 ---
        String orderId = String.format("ORD-BATCH-%d-%s", subscriptionId, paymentParams.getNextBillingDate().toString());


        TossAutomatedPaymentRequest tossRequest = TossAutomatedPaymentRequest.builder()
                .customerKey(paymentParams.getCustomerKey())
                .amount(paymentParams.getSnapshotAmount())
                .orderId(orderId)
                .orderName(paymentParams.getPlanName())
                .customerEmail(paymentParams.getCustomerEmail())
                .customerName(paymentParams.getCustomerName())
                .build();
        try {
            // 토스 결제 시도
            TossAutomatedPaymentResponse tossResponse = tossPaymentsClient.requestPayment(
                    paymentParams.getPlainBillingKey(),tossRequest
            );

            subscriptionBatchService.completeScheduledPayment(subscriptionId, orderId, tossResponse);

        } catch (Exception e) {
            // 실패 시 유예 상태로 변환 및 실패 이력 적재
            subscriptionBatchService.handleBatchPaymentFailure(
                    subscriptionId, orderId, paymentParams.getSnapshotAmount(), e.getMessage(), today, paymentParams.getOriginalStatus()
            );
            // todo [알림 발송 지점] 유저에게 실패 알림 전송

            throw e;
        }
    }

    public void finalizeSubscription(Long subscriptionId) {
        // 서비스의 트랜잭션 메서드를 호출하도록 위임
        subscriptionBatchService.finalizeSubscription(subscriptionId);
    }


}