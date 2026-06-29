package come.back.gotoday.payment.subscription.service;

import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.external.toss.dto.TossAutomatedPaymentRequest;
import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionBatchFacade 단위 테스트")
class SubscriptionBatchFacadeTest {

    @InjectMocks
    private SubscriptionBatchFacade subscriptionBatchFacade;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private SubscriptionBatchService subscriptionBatchService;

    private final Long SUBSCRIPTION_ID = 1L;
    private final int GRACE_PERIOD_DAYS = 7;

    private SubscriptionBatchService.BatchPaymentParameters.BatchPaymentParametersBuilder defaultParamsBuilder() {
        return SubscriptionBatchService.BatchPaymentParameters.builder()
                .isExpired(false)
                .nextBillingDate(LocalDate.of(2026, 6, 29))
                .customerKey("customer-key-123")
                .snapshotAmount(9900L)
                .planName("프리미엄 구독 요금제")
                .customerEmail("user@test.com")
                .customerName("홍길동")
                .plainBillingKey("toss-billing-key-abc")
                .originalStatus(SubscriptionStatus.ACTIVE);
    }

    @Nested
    @DisplayName("executeScheduledPayment 메서드는")
    class Describe_executeScheduledPayment {

        @Test
        @DisplayName("다른 스레드가 이미 선점하여 반환된 준비 파라미터가 null이라면 즉시 메서드를 탈출한다.")
        void it_returns_immediately_when_params_is_null() {
            // given
            when(subscriptionBatchService.lockAndPreparePayment(
                    eq(SUBSCRIPTION_ID), any(LocalDate.class), any(List.class), eq(GRACE_PERIOD_DAYS)
            )).thenReturn(null);

            // when
            subscriptionBatchFacade.executeScheduledPayment(SUBSCRIPTION_ID);

            // then
            verify(tossPaymentsClient, never()).requestPayment(anyString(), any(TossAutomatedPaymentRequest.class));
            verify(subscriptionBatchService, never()).completeScheduledPayment(anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("유예 기간 만료로 인해 서비스단에서 이미 해지된 건(isExpired=true)이라면 토스 결제를 시도하지 않고 종료한다.")
        void it_returns_immediately_when_subscription_is_expired() {
            // given
            SubscriptionBatchService.BatchPaymentParameters expiredParams = defaultParamsBuilder()
                    .isExpired(true)
                    .build();

            when(subscriptionBatchService.lockAndPreparePayment(
                    eq(SUBSCRIPTION_ID), any(LocalDate.class), any(List.class), eq(GRACE_PERIOD_DAYS)
            )).thenReturn(expiredParams);

            // when
            subscriptionBatchFacade.executeScheduledPayment(SUBSCRIPTION_ID);

            // then
            verify(tossPaymentsClient, never()).requestPayment(anyString(), any(TossAutomatedPaymentRequest.class));
            verify(subscriptionBatchService, never()).completeScheduledPayment(anyLong(), anyString(), any());
            verify(subscriptionBatchService, never()).handleBatchPaymentFailure(anyLong(), anyString(), anyLong(), anyString(), any(LocalDate.class), any());
        }

        @Test
        @DisplayName("정상 결제 대상인 경우 실제 도메인 파라미터 규격에 맞춰 토스페이먼츠에 결제를 요청하고 최종 완료 처리한다.")
        void it_requests_payment_to_toss_and_completes_safely() {
            // given
            SubscriptionBatchService.BatchPaymentParameters validParams = defaultParamsBuilder().build();
            TossAutomatedPaymentResponse mockResponse = mock(TossAutomatedPaymentResponse.class);

            when(subscriptionBatchService.lockAndPreparePayment(
                    eq(SUBSCRIPTION_ID), any(LocalDate.class), any(List.class), eq(GRACE_PERIOD_DAYS)
            )).thenReturn(validParams);

            when(tossPaymentsClient.requestPayment(eq("toss-billing-key-abc"), any(TossAutomatedPaymentRequest.class)))
                    .thenReturn(mockResponse);

            // when
            subscriptionBatchFacade.executeScheduledPayment(SUBSCRIPTION_ID);

            // then
            String expectedOrderId = "ORD-BATCH-1-2026-06-29";
            verify(subscriptionBatchService, times(1))
                    .completeScheduledPayment(SUBSCRIPTION_ID, expectedOrderId, mockResponse);
        }

        @Test
        @DisplayName("토스 결제 중 네트워크 타임아웃(ResourceAccessException)이 발생하면 PENDING 유지를 위해 실패 처리(유예 전환)를 건너뛴다.")
        void it_does_not_fail_when_network_timeout_occurs() {
            // given
            SubscriptionBatchService.BatchPaymentParameters validParams = defaultParamsBuilder().build();
            when(subscriptionBatchService.lockAndPreparePayment(
                    eq(SUBSCRIPTION_ID), any(LocalDate.class), any(List.class), eq(GRACE_PERIOD_DAYS)
            )).thenReturn(validParams);

            when(tossPaymentsClient.requestPayment(anyString(), any(TossAutomatedPaymentRequest.class)))
                    .thenThrow(new ResourceAccessException("Read timeout connecting to Toss Payments"));

            // when
            subscriptionBatchFacade.executeScheduledPayment(SUBSCRIPTION_ID);

            // then
            // 🚨 네트워크 장애 시 즉시 실패처리를 날리지 않아야 배치 정합성 스케줄러가 차후 수습 가능합니다.
            verify(subscriptionBatchService, never()).handleBatchPaymentFailure(
                    anyLong(), anyString(), anyLong(), anyString(), any(LocalDate.class), any()
            );
            verify(subscriptionBatchService, never()).completeScheduledPayment(anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("토스 결제가 한도 초과/잔액 부족 등으로 명확히 거절(RestClientResponseException)되면 즉시 배치 유예 실패 처리를 수행한다.")
        void it_handles_failure_when_toss_rejects_payment() {
            // given
            SubscriptionBatchService.BatchPaymentParameters validParams = defaultParamsBuilder().build();
            when(subscriptionBatchService.lockAndPreparePayment(
                    eq(SUBSCRIPTION_ID), any(LocalDate.class), any(List.class), eq(GRACE_PERIOD_DAYS)
            )).thenReturn(validParams);

            RestClientResponseException mockException = mock(RestClientResponseException.class);
            when(mockException.getResponseBodyAsString()).thenReturn("{\"code\":\"LACK_OF_BALANCE\",\"message\":\"잔액이 부족합니다.\"}");

            when(tossPaymentsClient.requestPayment(anyString(), any(TossAutomatedPaymentRequest.class)))
                    .thenThrow(mockException);

            // when
            subscriptionBatchFacade.executeScheduledPayment(SUBSCRIPTION_ID);

            // then
            String expectedOrderId = "ORD-BATCH-1-2026-06-29";
            verify(subscriptionBatchService, times(1)).handleBatchPaymentFailure(
                    eq(SUBSCRIPTION_ID),
                    eq(expectedOrderId),
                    eq(9900L),
                    contains("잔액이 부족합니다"),
                    any(LocalDate.class),
                    eq(SubscriptionStatus.ACTIVE)
            );
        }

        @Test
        @DisplayName("결제 프로세스 도중 예기치 못한 시스템 예외(Exception)가 던져지면 에러 내용을 담아 실패 처리를 기록한다.")
        void it_handles_failure_when_unexpected_exception_occurs() {
            // given
            SubscriptionBatchService.BatchPaymentParameters validParams = defaultParamsBuilder().build();
            when(subscriptionBatchService.lockAndPreparePayment(
                    eq(SUBSCRIPTION_ID), any(LocalDate.class), any(List.class), eq(GRACE_PERIOD_DAYS)
            )).thenReturn(validParams);

            when(tossPaymentsClient.requestPayment(anyString(), any(TossAutomatedPaymentRequest.class)))
                    .thenThrow(new RuntimeException("의문의 시스템 유실 장애"));

            // when
            subscriptionBatchFacade.executeScheduledPayment(SUBSCRIPTION_ID);

            // then
            String expectedOrderId = "ORD-BATCH-1-2026-06-29";
            verify(subscriptionBatchService, times(1)).handleBatchPaymentFailure(
                    eq(SUBSCRIPTION_ID),
                    eq(expectedOrderId),
                    eq(9900L),
                    eq("의문의 시스템 유실 장애"),
                    any(LocalDate.class),
                    eq(SubscriptionStatus.ACTIVE)
            );
        }
    }

    @Nested
    @DisplayName("finalizeSubscription 메서드는")
    class Describe_finalizeSubscription {

        @Test
        @DisplayName("최종 배치 해지 호출 시 내부 BatchService의 동일 위임 메서드로 요청을 정상 전달한다.")
        void it_delegates_to_batch_service() {
            // when
            subscriptionBatchFacade.finalizeSubscription(SUBSCRIPTION_ID);

            // then
            verify(subscriptionBatchService, times(1)).finalizeSubscription(SUBSCRIPTION_ID);
        }
    }
}