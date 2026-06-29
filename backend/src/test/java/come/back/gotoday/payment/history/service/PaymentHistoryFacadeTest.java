package come.back.gotoday.payment.history.service;

import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.external.toss.dto.TossCancelRequest;
import come.back.gotoday.external.toss.dto.TossCancelResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.subscription.dto.SubscriptionPaymentCancelRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentHistoryFacade 단위 테스트")
class PaymentHistoryFacadeTest {

    @InjectMocks
    private PaymentHistoryFacade paymentHistoryFacade;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private PaymentHistoryService paymentHistoryService;

    // 테이블 구조 기반 기본 픽스처 데이터
    private final Long MEMBER_ID = 1L;
    private final Long PAYMENT_HISTORY_ID = 100L;
    private final String VALID_PAYMENT_KEY = "tosspayments_key_20260629";
    private final String CANCEL_REASON = "사용자 변심으로 인한 정기 결제 취소";

    /**
     *제공된 TossCancelResponse Record 구조에 맞춘 가짜 객체 생성 헬퍼 메서드
     */
    private TossCancelResponse createMockTossResponse(String status) {
        return new TossCancelResponse(
                VALID_PAYMENT_KEY,
                "order_idx_12345",
                "한달 정기 구독 상품",
                status,
                9900L,
                "2026-06-29T10:00:00+09:00",
                "2026-06-29T10:01:00+09:00",
                Collections.emptyList(),
                new TossCancelResponse.ReceiptInfo("https://receipt.tosspayments.com/ans/123")
        );
    }

    @Nested
    @DisplayName("cancelPayment 메서드는")
    class Describe_cancelPayment {

        @Nested
        @DisplayName("정상적인 결제 정보와 토스 API 성공 응답이 주어지면")
        class Context_with_valid_request_and_successful_toss_response {

            @Test
            @DisplayName("토스 API를 호출하고 내부 DB의 결제 및 구독 상태를 취소(CANCELED) 상태로 변경한다.")
            void it_cancels_payment_and_subscription_successfully() {
                // given
                SubscriptionPaymentCancelRequest request = new SubscriptionPaymentCancelRequest(CANCEL_REASON);
                TossCancelResponse mockResponse = createMockTossResponse("CANCELED");

                when(paymentHistoryService.getPaymentKeyValidated(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .thenReturn(VALID_PAYMENT_KEY);

                when(tossPaymentsClient.cancelPayment(eq(VALID_PAYMENT_KEY), any(TossCancelRequest.class)))
                        .thenReturn(mockResponse);

                // when
                paymentHistoryFacade.cancelPayment(MEMBER_ID, PAYMENT_HISTORY_ID, request);

                // then
                verify(tossPaymentsClient, times(1))
                        .cancelPayment(eq(VALID_PAYMENT_KEY), argThat(cancelReq -> CANCEL_REASON.equals(cancelReq.cancelReason())));

                verify(paymentHistoryService, times(1))
                        .cancelPaymentAndSubscription(PAYMENT_HISTORY_ID, MEMBER_ID);
            }
        }

        @Nested
        @DisplayName("요청한 내부 결제 이력이 유효하지 않거나 본인 소유가 아니라면")
        class Context_with_invalid_payment_history_or_unauthorized_member {

            @Test
            @DisplayName("Service 계층의 예외를 그대로 던지고, 토스 API나 DB 수정 로직을 호출하지 않는다.")
            void it_throws_exception_from_service_and_stops_process() {
                // given
                SubscriptionPaymentCancelRequest request = new SubscriptionPaymentCancelRequest(CANCEL_REASON);

                when(paymentHistoryService.getPaymentKeyValidated(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .thenThrow(new BusinessException(ErrorCode.PAYMENT_HISTORY_NOT_FOUND));

                // when & then
                assertThatThrownBy(() -> paymentHistoryFacade.cancelPayment(MEMBER_ID, PAYMENT_HISTORY_ID, request))
                        .isInstanceOf(BusinessException.class);

                verify(tossPaymentsClient, never()).cancelPayment(any(), any());
                verify(paymentHistoryService, never()).cancelPaymentAndSubscription(any(), any());
            }
        }

        @Nested
        @DisplayName("토스페이먼츠 API 응답이 null이거나 결과 status가 'CANCELED'가 아니라면")
        class Context_with_failed_or_incomplete_toss_response {

            @Test
            @DisplayName("EXTERNAL_API_ERROR 예외를 던지고 내부 DB 상태 변경을 하지 않는다. (응답이 null인 경우)")
            void it_throws_external_api_error_when_response_is_null() {
                // given
                SubscriptionPaymentCancelRequest request = new SubscriptionPaymentCancelRequest(CANCEL_REASON);

                when(paymentHistoryService.getPaymentKeyValidated(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .thenReturn(VALID_PAYMENT_KEY);
                when(tossPaymentsClient.cancelPayment(eq(VALID_PAYMENT_KEY), any(TossCancelRequest.class)))
                        .thenReturn(null);

                // when & then
                assertThatThrownBy(() -> paymentHistoryFacade.cancelPayment(MEMBER_ID, PAYMENT_HISTORY_ID, request))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);

                verify(paymentHistoryService, never()).cancelPaymentAndSubscription(any(), any());
            }

            @Test
            @DisplayName("EXTERNAL_API_ERROR 예외를 던지고 내부 DB 상태 변경을 하지 않는다. (status가 'ABORTED' 등 다른 상태인 경우)")
            void it_throws_external_api_error_when_status_is_not_canceled() {
                // given
                SubscriptionPaymentCancelRequest request = new SubscriptionPaymentCancelRequest(CANCEL_REASON);
                TossCancelResponse mockPartialResponse = createMockTossResponse("ABORTED");

                when(paymentHistoryService.getPaymentKeyValidated(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .thenReturn(VALID_PAYMENT_KEY);
                when(tossPaymentsClient.cancelPayment(eq(VALID_PAYMENT_KEY), any(TossCancelRequest.class)))
                        .thenReturn(mockPartialResponse);

                // when & then
                assertThatThrownBy(() -> paymentHistoryFacade.cancelPayment(MEMBER_ID, PAYMENT_HISTORY_ID, request))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);

                verify(paymentHistoryService, never()).cancelPaymentAndSubscription(any(), any());
            }
        }

        @Nested
        @DisplayName("토스페이먼츠 API 호출 자체에서 외부 통신 장애 예외가 발생하면")
        class Context_with_toss_api_runtime_exception {

            @Test
            @DisplayName("예외가 외부로 전파되며 내부 DB 업데이트 로직은 차단된다.")
            void it_propagates_runtime_exception_and_does_not_update_db() {
                // given
                SubscriptionPaymentCancelRequest request = new SubscriptionPaymentCancelRequest(CANCEL_REASON);

                when(paymentHistoryService.getPaymentKeyValidated(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .thenReturn(VALID_PAYMENT_KEY);
                when(tossPaymentsClient.cancelPayment(eq(VALID_PAYMENT_KEY), any(TossCancelRequest.class)))
                        .thenThrow(new RuntimeException("Toss Server Connection Timeout"));

                // when & then
                assertThatThrownBy(() -> paymentHistoryFacade.cancelPayment(MEMBER_ID, PAYMENT_HISTORY_ID, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Toss Server Connection Timeout");

                verify(paymentHistoryService, never()).cancelPaymentAndSubscription(any(), any());
            }
        }
    }
}