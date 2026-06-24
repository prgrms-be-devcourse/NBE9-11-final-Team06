package come.back.gotoday.payment.subscription.service;

import come.back.gotoday.external.toss.dto.TossAutomatedPaymentRequest;
import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import come.back.gotoday.payment.idempotency.enums.IdempotencyStatus;
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
    public SubscriptionResponse startSubscription(Long memberId, SubscriptionRequest request, String idempotencyKey) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Plan plan =planRepository.findById(request.planId())
                .orElseThrow(()-> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        IdempotencyKey idempotencyKeyEntity = idempotencyManager.getOrCreateLock(
                member, idempotencyKey, "/v1/subscriptions", request.toString()
        );

        if (idempotencyKeyEntity.getStatus() == IdempotencyStatus.SUCCESS) {
            log.info("[Idempotency Hit] 이미 성공 처리된 정기 구독 요청입니다. Key: {}", idempotencyKey);
            return subscriptionService.getSubscriptionResponseFromJson(idempotencyKeyEntity.getResponseBody());
        }

        String orderId = null;
        boolean isPaymentApproved = false;
        TossAutomatedPaymentResponse tossResponse =null;
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
            tossResponse = tossPaymentsClient.requestPayment(
                    paymentParams.getPlainBillingKey(),
                    tossRequest
            );

            // 외부 결제가 성공했으므로 플래그를 true로 변경
            isPaymentApproved = true;

            // 결제 성공 이력 적재 및 구독 완전 활성화
            SubscriptionResponse response = subscriptionService.completeSubscription(orderId, tossResponse);

            idempotencyManager.updateToSuccess(idempotencyKeyEntity, 200, subscriptionService.convertResponseToJson(response));

            return response;

        } catch (Exception e) {
            log.error("정기 구독 및 첫 달 결제 처리 중 에러 발생. 주문ID: {}, 사유: {}", orderId, e.getMessage());

            if (isPaymentApproved) {
                // [시나리오 A] 외부 결제는 성공했으나 내부 DB 작업(completeSubscription 등)에서 에러가 발생한 경우
                // 멱등성 키를 UNKNOWN이나 중립 상태로 두거나, 특정 에러 코드로 기록하여 재시도 가능하게 함
                idempotencyManager.updateToFail(idempotencyKeyEntity, 500, "PAYMENT_SUCCESS_BUT_DB_ERROR: " + e.getMessage());

                if (orderId != null) {
                    // ★ 취소(CANCELED)가 아니라 '수동 정산/확인 필요' 상태로 변경하고, 관리자 알림을 보냄
                    subscriptionService.handlePaymentMismatch(orderId, plan.getAmount(), tossResponse, e.getMessage());
                }
            } else {
                // [시나리오 B] 외부 결제 자체가 실패했거나, 결제 요청 전에 에러가 발생한 경우
                idempotencyManager.updateToFail(idempotencyKeyEntity, 500, e.getMessage());

                if (orderId != null) {
                    // 기존 로직대로 안전하게 구독 실패/취소 처리
                    subscriptionService.handleSubscriptionFailure(orderId, plan.getAmount(), e.getMessage());
                }
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