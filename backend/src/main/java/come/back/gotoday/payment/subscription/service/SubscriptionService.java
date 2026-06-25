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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final BillingInfoRepository billingInfoRepository;
    private final PlanRepository planRepository;
    private final ObjectMapper objectMapper;
    /**
     *구독 정보 검증 및 주문 ID 조기 생성 (가결제 단계)
     */
    @Transactional
    public String prepareSubscription(Long memberId, SubscriptionRequest request) {
        BillingInfo billingInfo = billingInfoRepository.findByIdAndMemberId(request.billingInfoId(), memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_BILLING_INFO));

        // 추가: DB에서 정확한 요금제 엔티티를 조회하여 가격 및 활성화 검증 진행
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        boolean hasExistingSubscription = subscriptionRepository.existsByMemberIdAndStatusIn(
                memberId, java.util.List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING)
        );
        if (hasExistingSubscription) {
            throw new BusinessException(ErrorCode.DUPLICATE_ACTIVE_SUBSCRIPTION);
        }

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

        // 구독 상태를 PENDING -> ACTIVE로 변경하고 차기 결제일을 세팅
        subscription.activate();

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

        subscription.reserveCancellation();
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
        List<SubscriptionStatus> targetStatuses = List.of(
                SubscriptionStatus.ACTIVE,
                SubscriptionStatus.CANCELED_RESERVED
        );
        Subscription subscription = subscriptionRepository.findActiveSubscriptionByMemberId(memberId, targetStatuses)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVE_SUBSCRIPTION_NOT_FOUND));

        return SubscriptionResponse.from(subscription);
    }

    /**
     *  외부 API 결제는 성공했으나, 로컬 DB 반영 과정(completeSubscription)에서 예외가 발생한 경우의 보정 처리
     */
    @Transactional
    public void handlePaymentMismatch(String orderId, Long amount, TossAutomatedPaymentResponse tossResponse, String failureReason) {
        Long subscriptionId = extractSubscriptionId(orderId);

        subscriptionRepository.findById(subscriptionId).ifPresent(subscription -> {
            // 1. 구독 상태를 일반 취소(CANCELED)가 아닌 '수동 확인 대상'으로 변경
            // (만약 상태 추가가 어렵다면 기존의 PENDING 상태를 그대로 유지하셔도 됩니다)
            subscription.changeToManualCheck();

            // 2. 외부 결제는 성공했으므로, 실패 이력이 아닌 '성공 이력' 또는 '정산 필요 이력'으로 적재
            // tossResponse가 null이 아닐 가능성이 높지만, 만약 캐치 블록의 시점에 따라 다를 수 있으므로 방어 코드를 작성합니다.
            String paymentKey = (tossResponse != null) ? tossResponse.paymentKey() : "UNKNOWN_BUT_PAID";
            String receiptUrl = (tossResponse != null) ? tossResponse.getReceiptUrl() : "";

            // 비정상 흐름에서의 성공이므로 비고(failureReason) 등을 남길 수 있도록 처리하거나,
            // 기존 successHistory 생성 메서드를 활용하되 로그를 결합합니다.
            PaymentHistory mismatchHistory = PaymentHistory.createSuccessHistory(
                    subscription,
                    paymentKey,
                    orderId,
                    amount,
                    receiptUrl
            );

            // 데이터 보존을 위해 에러 로그 메시지를 기록하고 싶다면 History 엔티ti 설계에 따라 필드를 채웁니다.
            paymentHistoryRepository.save(mismatchHistory);

            // 3. 대시보드 인지용 통합 로그 출력 및 알림 Trigger (Slack 등 Hook 연동 권장)
            log.error("[CRITICAL DATA MISMATCH] 토스 결제는 성공했으나 내부 DB 갱신 중 에러가 발생하여 수동 정산 대상으로 분류합니다. " +
                    "주문ID: {}, 결제Key: {}, 사유: {}", orderId, paymentKey, failureReason);
        });
    }

    public String convertResponseToJson(SubscriptionResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return response.toString();
        }
    }

    public SubscriptionResponse getSubscriptionResponseFromJson(String json) {
        try {
            return objectMapper.readValue(json, SubscriptionResponse.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Getter
    @Builder
    public static class PaymentParameters {
        private final String plainBillingKey;
        private final String customerKey;
    }
}
