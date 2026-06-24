package come.back.gotoday.payment.subscription.service;

import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.billing.entity.BillingInfo;
import come.back.gotoday.payment.billing.repository.BillingInfoRepository;
import come.back.gotoday.payment.plan.entity.Plan;
import come.back.gotoday.payment.plan.repository.PlanRepository;
import come.back.gotoday.payment.subscription.dto.SubscriptionRequest;
import come.back.gotoday.payment.subscription.dto.SubscriptionResponse;
import come.back.gotoday.payment.subscription.entity.PaymentHistory;
import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.subscription.repository.PaymentHistoryRepository;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final BillingInfoRepository billingInfoRepository;
    private final PlanRepository planRepository;
    /**
     * 1단계: 구독 정보 검증 및 주문 ID 조기 생성 (가결제 단계)
     */
    @Transactional
    public String prepareSubscription(Long memberId, SubscriptionRequest request) {
        BillingInfo billingInfo = billingInfoRepository.findByIdAndMemberId(request.billingInfoId(), memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_BILLING_INFO));

        // 추가: DB에서 정확한 요금제 엔티티를 조회하여 가격 및 활성화 검증 진행
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        subscriptionRepository.findActiveSubscriptionByMemberId(memberId, SubscriptionStatus.ACTIVE)
                .ifPresent(s -> { throw new BusinessException(ErrorCode.DUPLICATE_ACTIVE_SUBSCRIPTION); });

        Subscription subscription = Subscription.startSubscription(billingInfo, plan, plan.getAmount(),LocalDate.now());
        subscriptionRepository.save(subscription);

        return "ORD-SUB-" + subscription.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }


     //외부 API 호출에 필요한 평문 빌링키
    public PaymentParameters getPaymentParameters(Long memberId, Long billingInfoId) {
        BillingInfo billingInfo = billingInfoRepository.findByIdAndMemberId(billingInfoId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_BILLING_INFO));

        return PaymentParameters.builder()
                .plainBillingKey(billingInfo.getBillingKey()) // Converter 자동 복호화
                .customerKey(billingInfo.getCustomerKey())
                .build();
    }

    //외부 결제 성공 데이터 기반 최종 승인 및 갱신
    @Transactional
    public SubscriptionResponse completeSubscription(String orderId, TossAutomatedPaymentResponse tossResponse) {
        Long subscriptionId = extractSubscriptionId(orderId);
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 결제 성공 이력 저장
        PaymentHistory successHistory = PaymentHistory.createSuccessHistory(
                subscription,
                tossResponse.paymentKey(),
                orderId,
                tossResponse.totalAmount(),
                tossResponse.getReceiptUrl()
        );
        paymentHistoryRepository.save(successHistory);

        return SubscriptionResponse.from(subscription);
    }

    //외부 API 승인 실패 시 복구 비즈니스 (구독 무효화 및 실패 이력 적재
    @Transactional
    public void handleSubscriptionFailure(String orderId, Long amount, String failureReason) {
        Long subscriptionId = extractSubscriptionId(orderId);
        subscriptionRepository.findById(subscriptionId).ifPresent(subscription -> {
            // 첫 달 결제가 실패했으므로 구독 권한 CANCELED 처리
            subscription.cancel();

            // 실패 이력 저장하여 추적성 확보
            PaymentHistory failureHistory = PaymentHistory.createFailureHistory(
                    subscription,
                    orderId,
                    amount,
                    failureReason
            );
            paymentHistoryRepository.save(failureHistory);
        });
    }

    @Transactional
    public void cancelSubscription(Long memberId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getBillingInfo().getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_SUBSCRIPTION_ACCESS);
        }

        subscription.cancel();
    }

    private Long extractSubscriptionId(String orderId) {
        try {
            // "ORD-SUB-{id}-******" 구조에서 id 추출
            return Long.parseLong(orderId.split("-")[2]);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_ID_FORMAT);
        }
    }

    //현재 회원의 활성화된 구독 정보 조회
    public SubscriptionResponse getActiveSubscription(Long memberId) {
        Subscription subscription = subscriptionRepository.findActiveSubscriptionByMemberId(memberId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVE_SUBSCRIPTION_NOT_FOUND));

        return SubscriptionResponse.from(subscription);
    }

    @Getter
    @Builder
    public static class PaymentParameters {
        private final String plainBillingKey;
        private final String customerKey;
    }
}
