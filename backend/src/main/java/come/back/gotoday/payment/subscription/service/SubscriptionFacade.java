package come.back.gotoday.payment.subscription.service;

import come.back.gotoday.external.toss.dto.TossAutomatedPaymentRequest;
import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import come.back.gotoday.payment.idempotency.service.IdempotencyManager;
import come.back.gotoday.payment.plan.entity.Plan;
import come.back.gotoday.payment.plan.repository.PlanRepository;
import come.back.gotoday.payment.subscription.dto.SubscriptionRequest;
import come.back.gotoday.payment.subscription.dto.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionFacade {

    private final TossPaymentsClient tossPaymentsClient;
    private final SubscriptionService subscriptionService;
    private final IdempotencyManager idempotencyManager;
    private final MemberRepository memberRepository;
    private final PlanRepository planRepository;
    /**
     * 정기 구독 신청 및 첫 달 즉시 결제 승인
     */
    public SubscriptionResponse startSubscription(Long memberId, SubscriptionRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Plan plan =planRepository.findById(request.planId())
                .orElseThrow(()-> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        String internalIdempotencyKey = String.format("SUB_INIT_%d_%s", memberId, java.util.UUID.randomUUID().toString().replace("-", ""));
        //todo 이미 processing, success 상태일때, 이미 처리된 값이라고 early return 처리

        IdempotencyKey idempotencyKeyEntity = idempotencyManager.getOrCreateLock(
                member, internalIdempotencyKey, "/v1/subscriptions", request.toString()
        );

        String orderId = null;
        try {

            orderId = subscriptionService.prepareSubscription(memberId, request);

            // 토스 API 요청 데이터 빌드에 필요한 평문 빌링키 및 메타데이터 조회
            var paymentParams = subscriptionService.getPaymentParameters(memberId, request.billingInfoId());

            TossAutomatedPaymentRequest tossRequest = TossAutomatedPaymentRequest.builder()
                    .customerKey(paymentParams.getCustomerKey())
                    .amount(plan.getAmount())
                    .orderId(orderId)
                    .orderName(plan.getName())
                    .customerEmail(member.getEmail())
                    .customerName(member.getNickname())
                    .build();

            // 토스페이먼츠 빌링키 결제 승인 API 호출
            TossAutomatedPaymentResponse tossResponse = tossPaymentsClient.requestPayment(
                    paymentParams.getPlainBillingKey(),
                    tossRequest
            );

            // 결제 성공 이력 적재 및 구독 완전 활성화
            SubscriptionResponse response = subscriptionService.completeSubscription(orderId, tossResponse);

            idempotencyManager.updateToSuccess(idempotencyKeyEntity, 200, response.toString());

            return response;

        } catch (Exception e) {
            log.error("정기 구독 및 첫 달 결제 처리 중 에러 발생. 주문ID: {}, 사유: {}", orderId, e.getMessage());

            // 7. 실패 시 멱등성 FAIL 상태 변경 및 실패 이력 내부 영속화
            idempotencyManager.updateToFail(idempotencyKeyEntity, 500, e.getMessage());

            if (orderId != null) {
                // 외부 통신 실패 혹은 비즈니스 예외에 따른 구독 취소/실패 이력 적재 처리
                subscriptionService.handleSubscriptionFailure(orderId, plan.getAmount(), e.getMessage());
            }
            throw e;
        }
    }

    /**
     * 정기 구독 해지
     */
    public void cancelSubscription(Long memberId, Long subscriptionId) {
        subscriptionService.cancelSubscription(memberId, subscriptionId);
    }

    public SubscriptionResponse getMyActiveSubscription(Long memberId) {
        return subscriptionService.getActiveSubscription(memberId);
    }
}