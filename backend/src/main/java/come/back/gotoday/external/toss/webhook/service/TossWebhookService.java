package come.back.gotoday.external.toss.webhook.service;

import come.back.gotoday.external.toss.webhook.dto.TossBillingWebhookRequest;
import come.back.gotoday.external.toss.webhook.dto.TossPaymentWebhookRequest;
import come.back.gotoday.external.toss.webhook.dto.TossWebhookRequest;
import come.back.gotoday.payment.billing.entity.BillingInfo;
import come.back.gotoday.payment.billing.enums.BillingStatus;
import come.back.gotoday.payment.billing.repository.BillingInfoRepository;
import come.back.gotoday.payment.history.entity.PaymentHistory;
import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.history.enums.PaymentStatus;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.history.repository.PaymentHistoryRepository;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TossWebhookService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final BillingInfoRepository billingInfoRepository; // 추가
    private final SubscriptionRepository subscriptionRepository;

    public void handleWebhook(TossWebhookRequest request) {
        log.info("[토스 웹훅 수신] EventType: {}", request.eventType());

        if (request instanceof TossPaymentWebhookRequest paymentWebhook) {
            processPaymentCancelStatus(paymentWebhook.data());
        } else if (request instanceof TossBillingWebhookRequest billingWebhook) {
            processBillingDeleted(billingWebhook.data());
        } else {
            log.warn("[토스 웹훅] 처리되지 않은 이벤트 타입: {}", request.eventType());
        }
    }

    /**
     *  결제 취소 처리
     */
    private void processPaymentCancelStatus(TossPaymentWebhookRequest.PaymentData data) {
        String tossStatus = data.status();

        if ("CANCELED".equals(tossStatus) || "PARTIAL_CANCELED".equals(tossStatus)) {
            PaymentHistory paymentHistory = paymentHistoryRepository.findByOrderIdForUpdate(data.orderId())
                    .orElse(null);

            if (paymentHistory == null) {
                log.warn("[토스 웹훅] 결제 이력이 존재하지 않습니다. OrderId: {}", data.orderId());
                return;
            }

            if (paymentHistory.getStatus() == PaymentStatus.CANCELED) {
                return;
            }

            paymentHistory.cancel();
            Subscription subscription = paymentHistory.getSubscription();
            if (subscription != null) {
                subscription.cancel();
            }
            log.info("[토스 웹훅] 결제 취소 반영으로 인한 구독 해지 완료. OrderId: {}", data.orderId());
        }
    }

    /**
     *  외부(토스앱 등)에서 빌링키를 삭제한 경우
     */
    private void processBillingDeleted(TossBillingWebhookRequest.BillingData data) {
        String billingKey = data.billingKey();
        log.info("[토스 웹훅] BILLING_DELETED 처리 시작. BillingKey: {}, 사유: {}", billingKey, data.reason());

        //  토스에서 넘겨준 plain billingKey로 우리 DB의 ACTIVE 카드가 있는지 조회
        BillingInfo billingInfo = billingInfoRepository.findByBillingKeyAndStatus(billingKey, BillingStatus.ACTIVE)
                .orElse(null);

        if (billingInfo == null) {
            log.info("[토스 웹훅 멱등 가드] 이미 내부에서 삭제되었거나 존재하지 않는 카드입니다.");
            return;
        }

        // 카드 소프트 딜리트 상태 변경
        billingInfo.delete();

        //  연동된 활성화 구독 차단
        List<Subscription> activeSubscriptions = subscriptionRepository
                .findAllByBillingInfoIdAndStatus(billingInfo.getId(), SubscriptionStatus.ACTIVE);

        for (Subscription subscription : activeSubscriptions) {
            subscription.cancel();
            log.info("[토스 웹훅] 외부 카드 해지로 인해 구독 강제 취소. Subscription ID: {}", subscription.getId());
        }
    }
}