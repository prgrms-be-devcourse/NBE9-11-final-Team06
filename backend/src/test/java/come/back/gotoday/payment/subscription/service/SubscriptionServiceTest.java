package come.back.gotoday.payment.subscription.service;

import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.billing.entity.BillingInfo;
import come.back.gotoday.payment.billing.repository.BillingInfoRepository;
import come.back.gotoday.payment.history.repository.PaymentHistoryRepository;
import come.back.gotoday.payment.plan.entity.Plan;
import come.back.gotoday.payment.plan.repository.PlanRepository;
import come.back.gotoday.payment.subscription.dto.SubscriptionRequest;
import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService 서비스 로직 테스트")
class SubscriptionServiceTest {

    @InjectMocks private SubscriptionService subscriptionService;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private BillingInfoRepository billingInfoRepository;
    @Mock private PlanRepository planRepository;
    @Mock private ObjectMapper objectMapper;

    @Test
    @DisplayName("구독 준비: 이미 활성화된 구독이 있으면 예외 발생")
    void prepareSubscription_DuplicateCheck() {
        SubscriptionRequest request = new SubscriptionRequest(1L, 1L);
        when(billingInfoRepository.findByIdAndMemberId(any(), any())).thenReturn(Optional.of(mock(BillingInfo.class)));
        when(planRepository.findById(any())).thenReturn(Optional.of(mock(Plan.class)));
        when(subscriptionRepository.existsByMemberIdAndStatusIn(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> subscriptionService.prepareSubscription(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_ACTIVE_SUBSCRIPTION);
    }

    @Test
    @DisplayName("결제 실패 핸들링: 구독 취소 및 실패 이력 저장")
    void handleSubscriptionFailure() {
        String orderId = "ORD-SUB-123-uuid";
        Subscription mockSub = mock(Subscription.class);
        when(subscriptionRepository.findById(123L)).thenReturn(Optional.of(mockSub));

        subscriptionService.handleSubscriptionFailure(orderId, 9900L, "TOSS_ERROR");

        verify(mockSub).cancel();
        verify(paymentHistoryRepository).save(any());
    }

    @Test
    @DisplayName("결제 미스매치: DB 반영 실패 시 수동 정산 모드로 변경")
    void handlePaymentMismatch() {
        String orderId = "ORD-SUB-123-uuid";
        Subscription mockSub = mock(Subscription.class);
        when(subscriptionRepository.findById(123L)).thenReturn(Optional.of(mockSub));

        subscriptionService.handlePaymentMismatch(orderId, 9900L, mock(TossAutomatedPaymentResponse.class), "DB_FAIL");

        verify(mockSub).changeToManualCheck();
        verify(paymentHistoryRepository).save(any());
    }

    @Test
    @DisplayName("JSON 유틸: 객체 직렬화 실패 시 toString 반환 처리")
    void convertResponseToJson_Fallback() throws Exception {
        // given
        var response = mock(come.back.gotoday.payment.subscription.dto.SubscriptionResponse.class);
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON Error"));
        when(response.toString()).thenReturn("fallback-string");

        // when
        String result = subscriptionService.convertResponseToJson(response);

        // then
        assertThat(result).isEqualTo("fallback-string");
    }

    @Test
    @DisplayName("JSON 유틸: JSON 파싱 실패 시 서버 에러 발생")
    void getSubscriptionResponseFromJson_Exception() throws Exception {
        when(objectMapper.readValue(anyString(), any(Class.class))).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> subscriptionService.getSubscriptionResponseFromJson("{}"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
    }
}