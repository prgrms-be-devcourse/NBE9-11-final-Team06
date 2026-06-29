package come.back.gotoday.payment.subscription.service;

import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import come.back.gotoday.payment.idempotency.enums.IdempotencyStatus;
import come.back.gotoday.payment.idempotency.service.IdempotencyManager;
import come.back.gotoday.payment.plan.entity.Plan;
import come.back.gotoday.payment.plan.repository.PlanRepository;
import come.back.gotoday.payment.subscription.dto.SubscriptionRequest;
import come.back.gotoday.payment.subscription.dto.SubscriptionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionFacade")
class SubscriptionFacadeTest {

    @InjectMocks
    private SubscriptionFacade subscriptionFacade;

    @Mock private TossPaymentsClient tossPaymentsClient;
    @Mock private SubscriptionService subscriptionService;
    @Mock private IdempotencyManager idempotencyManager;
    @Mock private MemberRepository memberRepository;
    @Mock private PlanRepository planRepository;

    private final Long memberId = 1L;
    private final Long planId = 10L;
    private final Long billingInfoId = 55L;
    private final String idempotencyKey = "key-123";

    @Test
    @DisplayName("성공: 결제 및 구독 승인 정상 처리")
    void startSubscription_Success() {
        SubscriptionRequest request = new SubscriptionRequest(billingInfoId, planId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mock(Member.class)));
        when(planRepository.findById(planId)).thenReturn(Optional.of(mock(Plan.class)));

        IdempotencyKey keyEntity = mock(IdempotencyKey.class);
        when(idempotencyManager.getOrCreateLock(any(), eq(idempotencyKey), any(), any())).thenReturn(keyEntity);
        when(keyEntity.getStatus()).thenReturn(IdempotencyStatus.PROCESSING);

        String orderId = "ORD-SUB-1";
        when(subscriptionService.prepareSubscription(memberId, request)).thenReturn(orderId);

        SubscriptionService.PaymentParameters params = SubscriptionService.PaymentParameters.builder()
                .customerKey("c1").plainBillingKey("b1").build();
        when(subscriptionService.getPaymentParameters(memberId, billingInfoId)).thenReturn(params);

        TossAutomatedPaymentResponse tossResponse = mock(TossAutomatedPaymentResponse.class);
        when(tossPaymentsClient.requestPayment(eq("b1"), any())).thenReturn(tossResponse);

        SubscriptionResponse expected = mock(SubscriptionResponse.class);
        when(subscriptionService.completeSubscription(orderId, tossResponse)).thenReturn(expected);

        SubscriptionResponse result = subscriptionFacade.startSubscription(memberId, request, idempotencyKey);

        assertThat(result).isEqualTo(expected);
        verify(idempotencyManager).updateToSuccess(eq(keyEntity), eq(200), any());
    }

    @Test
    @DisplayName("멱등성: 이미 성공한 요청은 중복 결제 없이 결과 반환")
    void startSubscription_IdempotencyHit() {
        SubscriptionRequest request = new SubscriptionRequest(billingInfoId, planId);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mock(Member.class)));
        when(planRepository.findById(planId)).thenReturn(Optional.of(mock(Plan.class)));

        IdempotencyKey keyEntity = mock(IdempotencyKey.class);
        when(idempotencyManager.getOrCreateLock(any(), eq(idempotencyKey), any(), any())).thenReturn(keyEntity);
        when(keyEntity.getStatus()).thenReturn(IdempotencyStatus.SUCCESS);
        when(keyEntity.getResponseBody()).thenReturn("{}");

        SubscriptionResponse cached = mock(SubscriptionResponse.class);
        when(subscriptionService.getSubscriptionResponseFromJson("{}")).thenReturn(cached);

        SubscriptionResponse result = subscriptionFacade.startSubscription(memberId, request, idempotencyKey);

        assertThat(result).isEqualTo(cached);
        verify(tossPaymentsClient, never()).requestPayment(any(), any());
    }

    @Test
    @DisplayName("예외(A): 외부 결제 성공 후 DB 반영 실패 시 미스매치 핸들러 호출")
    void startSubscription_DBError_After_Payment() {
        SubscriptionRequest request = new SubscriptionRequest(billingInfoId, planId);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mock(Member.class)));
        when(planRepository.findById(planId)).thenReturn(Optional.of(mock(Plan.class)));
        when(idempotencyManager.getOrCreateLock(any(), any(), any(), any())).thenReturn(mock(IdempotencyKey.class));
        when(subscriptionService.prepareSubscription(any(), any())).thenReturn("ORD-SUB-1");
        when(subscriptionService.getPaymentParameters(any(), any())).thenReturn(mock(SubscriptionService.PaymentParameters.class));

        when(tossPaymentsClient.requestPayment(any(), any())).thenReturn(mock(TossAutomatedPaymentResponse.class));
        when(subscriptionService.completeSubscription(any(), any())).thenThrow(new RuntimeException("DB ERROR"));

        assertThatThrownBy(() -> subscriptionFacade.startSubscription(memberId, request, idempotencyKey))
                .isInstanceOf(RuntimeException.class);

        verify(subscriptionService).handlePaymentMismatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("예외(B): 외부 결제 요청 자체 실패 시 구독 실패 핸들러 호출")
    void startSubscription_PaymentFailure() {
        SubscriptionRequest request = new SubscriptionRequest(billingInfoId, planId);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mock(Member.class)));
        when(planRepository.findById(planId)).thenReturn(Optional.of(mock(Plan.class)));
        when(idempotencyManager.getOrCreateLock(any(), any(), any(), any())).thenReturn(mock(IdempotencyKey.class));
        when(subscriptionService.prepareSubscription(any(), any())).thenReturn("ORD-SUB-1");
        when(subscriptionService.getPaymentParameters(any(), any())).thenReturn(mock(SubscriptionService.PaymentParameters.class));
        when(tossPaymentsClient.requestPayment(any(), any())).thenThrow(new RuntimeException("API ERROR"));

        assertThatThrownBy(() -> subscriptionFacade.startSubscription(memberId, request, idempotencyKey))
                .isInstanceOf(RuntimeException.class);

        verify(subscriptionService).handleSubscriptionFailure(any(), any(), eq("API ERROR"));
    }
}