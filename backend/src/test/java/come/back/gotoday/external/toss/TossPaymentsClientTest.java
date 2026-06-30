package come.back.gotoday.external.toss;

import come.back.gotoday.external.toss.dto.*;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.billing.dto.TossBillingKeyResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class TossPaymentsClientTest {

    @Test
    @DisplayName("테스트용 생성자는 주입받은 RestClient를 사용한다")
    void constructorUsesInjectedRestClient() {
        RestClient restClient = mock(RestClient.class);
        TossErrorHandler tossErrorHandler = mock(TossErrorHandler.class);

        TossPaymentsClient client = new TossPaymentsClient(
                "test-secret-key",
                tossErrorHandler,
                restClient
        );

        assertThat(ReflectionTestUtils.getField(client, "restClient"))
                .isSameAs(restClient);
        assertThat(ReflectionTestUtils.getField(client, "secretKey"))
                .isEqualTo("test-secret-key");
        assertThat(ReflectionTestUtils.getField(client, "tossErrorHandler"))
                .isSameAs(tossErrorHandler);
    }


    @Test
    @DisplayName("빌링키 발급 네트워크 재시도 실패는 최종 네트워크 예외로 변환한다")
    void recoverThrowsFinalNetworkErrorForBillingKeyRequest() {
        TossPaymentsClient client = client();

        assertThatThrownBy(() -> client.recover(
                new ResourceAccessException("connection timeout"),
                "idem-1",
                "auth-key",
                "customer-1"
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("빌링키 발급 BusinessException 복구는 기존 예외를 그대로 전파한다")
    void recoverRethrowsBusinessExceptionForBillingKeyRequest() {
        TossPaymentsClient client = client();
        BusinessException exception = new BusinessException(ErrorCode.EXTERNAL_API_ERROR);

        assertThatThrownBy(() -> client.recover(exception, "idem-1", "auth-key", "customer-1"))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("빌링키 삭제 네트워크 재시도 실패는 최종 네트워크 예외로 변환한다")
    void recoverDeleteThrowsFinalNetworkError() {
        TossPaymentsClient client = client();

        assertThatThrownBy(() -> client.recoverDelete(
                new ResourceAccessException("connection timeout"),
                "billing-key"
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("빌링키 삭제 BusinessException 복구는 기존 예외를 그대로 전파한다")
    void recoverDeleteRethrowsBusinessException() {
        TossPaymentsClient client = client();
        BusinessException exception = new BusinessException(ErrorCode.EXTERNAL_API_ERROR);

        assertThatThrownBy(() -> client.recoverDelete(exception, "billing-key"))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("자동 결제 네트워크 재시도 실패는 최종 네트워크 예외로 변환한다")
    void recoverPaymentThrowsFinalNetworkError() {
        TossPaymentsClient client = client();
        TossAutomatedPaymentRequest request = mock(TossAutomatedPaymentRequest.class);

        assertThatThrownBy(() -> client.recoverPayment(
                new ResourceAccessException("connection timeout"),
                "billing-key",
                request
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("자동 결제 BusinessException 복구는 기존 예외를 그대로 전파한다")
    void recoverPaymentRethrowsBusinessException() {
        TossPaymentsClient client = client();
        TossAutomatedPaymentRequest request = mock(TossAutomatedPaymentRequest.class);
        BusinessException exception = new BusinessException(ErrorCode.EXTERNAL_API_ERROR);

        assertThatThrownBy(() -> client.recoverPayment(exception, "billing-key", request))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("결제 취소 네트워크 재시도 실패는 최종 네트워크 예외로 변환한다")
    void recoverCancelThrowsFinalNetworkError() {
        TossPaymentsClient client = client();
        TossCancelRequest request = mock(TossCancelRequest.class);

        assertThatThrownBy(() -> client.recoverCancel(
                new ResourceAccessException("connection timeout"),
                "payment-key",
                request
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 취소 BusinessException 복구는 기존 예외를 그대로 전파한다")
    void recoverCancelRethrowsBusinessException() {
        TossPaymentsClient client = client();
        TossCancelRequest request = mock(TossCancelRequest.class);
        BusinessException exception = new BusinessException(ErrorCode.EXTERNAL_API_ERROR);

        assertThatThrownBy(() -> client.recoverCancel(exception, "payment-key", request))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("정산 조회 네트워크 재시도 실패는 최종 네트워크 예외로 변환한다")
    void recoverSettlementThrowsFinalNetworkError() {
        TossPaymentsClient client = client();
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 31);

        assertThatThrownBy(() -> client.recoverSettlement(
                new ResourceAccessException("connection timeout"),
                startDate,
                endDate
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("정산 조회 BusinessException 복구는 기존 예외를 그대로 전파한다")
    void recoverSettlementRethrowsBusinessException() {
        TossPaymentsClient client = client();
        BusinessException exception = new BusinessException(ErrorCode.EXTERNAL_API_ERROR);

        assertThatThrownBy(() -> client.recoverSettlement(
                exception,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).isSameAs(exception);
    }

    @Test
    @DisplayName("빌링키 발급 중 알 수 없는 예외는 외부 API 예외로 변환한다")
    void requestBillingKeyConvertsUnexpectedException() {
        RestClient restClient = mock(RestClient.class);
        given(restClient.post()).willThrow(new IllegalStateException("unexpected"));

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        assertThatThrownBy(() -> client.requestBillingKey("idem-1", "auth-key", "customer-1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("빌링키 발급 HTTP 오류는 TossErrorHandler에 위임한 뒤 그대로 전파한다")
    void requestBillingKeyDelegatesHttpErrorToHandler() {
        RestClient restClient = mock(RestClient.class);
        TossErrorHandler errorHandler = mock(TossErrorHandler.class);
        org.springframework.web.client.RestClientResponseException exception = mock(org.springframework.web.client.RestClientResponseException.class);
        given(restClient.post()).willThrow(exception);

        TossPaymentsClient client = client(restClient, errorHandler);

        assertThatThrownBy(() -> client.requestBillingKey("idem-1", "auth-key", "customer-1"))
                .isSameAs(exception);
        verify(errorHandler).handleTossError(exception);
    }

    @Test
    @DisplayName("빌링키 삭제 중 알 수 없는 예외는 외부 API 예외로 변환한다")
    void deleteBillingKeyConvertsUnexpectedException() {
        RestClient restClient = mock(RestClient.class);
        given(restClient.delete()).willThrow(new IllegalStateException("unexpected"));

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        assertThatThrownBy(() -> client.deleteBillingKeyFromServer("billing-key"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("빌링키 삭제 HTTP 오류는 TossErrorHandler에 위임한 뒤 그대로 전파한다")
    void deleteBillingKeyDelegatesHttpErrorToHandler() {
        RestClient restClient = mock(RestClient.class);
        TossErrorHandler errorHandler = mock(TossErrorHandler.class);
        org.springframework.web.client.RestClientResponseException exception = mock(org.springframework.web.client.RestClientResponseException.class);
        given(restClient.delete()).willThrow(exception);

        TossPaymentsClient client = client(restClient, errorHandler);

        assertThatThrownBy(() -> client.deleteBillingKeyFromServer("billing-key"))
                .isSameAs(exception);
        verify(errorHandler).handleTossError(exception);
    }

    @Test
    @DisplayName("자동 결제 승인 중 알 수 없는 예외는 외부 API 예외로 변환한다")
    void requestPaymentConvertsUnexpectedException() {
        RestClient restClient = mock(RestClient.class);
        given(restClient.post()).willThrow(new IllegalStateException("unexpected"));
        TossAutomatedPaymentRequest request = mock(TossAutomatedPaymentRequest.class);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        assertThatThrownBy(() -> client.requestPayment("billing-key", request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("자동 결제 승인 HTTP 오류는 TossErrorHandler에 위임한 뒤 그대로 전파한다")
    void requestPaymentDelegatesHttpErrorToHandler() {
        RestClient restClient = mock(RestClient.class);
        TossErrorHandler errorHandler = mock(TossErrorHandler.class);
        org.springframework.web.client.RestClientResponseException exception = mock(org.springframework.web.client.RestClientResponseException.class);
        given(restClient.post()).willThrow(exception);
        TossAutomatedPaymentRequest request = mock(TossAutomatedPaymentRequest.class);

        TossPaymentsClient client = client(restClient, errorHandler);

        assertThatThrownBy(() -> client.requestPayment("billing-key", request))
                .isSameAs(exception);
        verify(errorHandler).handleTossError(exception);
    }

    @Test
    @DisplayName("결제 취소 중 알 수 없는 예외는 외부 API 예외로 변환한다")
    void cancelPaymentConvertsUnexpectedException() {
        RestClient restClient = mock(RestClient.class);
        given(restClient.post()).willThrow(new IllegalStateException("unexpected"));
        TossCancelRequest request = mock(TossCancelRequest.class);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        assertThatThrownBy(() -> client.cancelPayment("payment-key", request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 취소 HTTP 오류는 TossErrorHandler에 위임한 뒤 그대로 전파한다")
    void cancelPaymentDelegatesHttpErrorToHandler() {
        RestClient restClient = mock(RestClient.class);
        TossErrorHandler errorHandler = mock(TossErrorHandler.class);
        org.springframework.web.client.RestClientResponseException exception = mock(org.springframework.web.client.RestClientResponseException.class);
        given(restClient.post()).willThrow(exception);
        TossCancelRequest request = mock(TossCancelRequest.class);

        TossPaymentsClient client = client(restClient, errorHandler);

        assertThatThrownBy(() -> client.cancelPayment("payment-key", request))
                .isSameAs(exception);
        verify(errorHandler).handleTossError(exception);
    }

    @Test
    @DisplayName("정산 조회 중 알 수 없는 예외는 외부 API 예외로 변환한다")
    void fetchSettlementsConvertsUnexpectedException() {
        RestClient restClient = mock(RestClient.class);
        given(restClient.get()).willThrow(new IllegalStateException("unexpected"));

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        assertThatThrownBy(() -> client.fetchSettlements(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("정산 조회 HTTP 오류는 TossErrorHandler에 위임한 뒤 그대로 전파한다")
    void fetchSettlementsDelegatesHttpErrorToHandler() {
        RestClient restClient = mock(RestClient.class);
        TossErrorHandler errorHandler = mock(TossErrorHandler.class);
        org.springframework.web.client.RestClientResponseException exception = mock(org.springframework.web.client.RestClientResponseException.class);
        given(restClient.get()).willThrow(exception);

        TossPaymentsClient client = client(restClient, errorHandler);

        assertThatThrownBy(() -> client.fetchSettlements(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).isSameAs(exception);
        verify(errorHandler).handleTossError(exception);
    }

    @Test
    @DisplayName("주문 ID 결제 조회 중 알 수 없는 예외는 외부 API 예외로 변환한다")
    void getPaymentByOrderIdConvertsUnexpectedException() {
        RestClient restClient = mock(RestClient.class);
        given(restClient.get()).willThrow(new IllegalStateException("unexpected"));

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        assertThatThrownBy(() -> client.getPaymentByOrderId("order-1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("주문 ID 결제 조회 HTTP 오류는 호출부 제어를 위해 그대로 전파한다")
    void getPaymentByOrderIdRethrowsHttpError() {
        RestClient restClient = mock(RestClient.class);
        org.springframework.web.client.RestClientResponseException exception = mock(org.springframework.web.client.RestClientResponseException.class);
        given(restClient.get()).willThrow(exception);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        assertThatThrownBy(() -> client.getPaymentByOrderId("order-1"))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("빌링키 발급 성공 시 토스 응답을 반환하고 인증 및 멱등성 헤더를 전달한다")
    void requestBillingKeyReturnsTossResponse() {
        RestClient restClient = mock(RestClient.class);
        RestClient.ResponseSpec responseSpec = stubPostResponse(restClient);
        TossBillingKeyResponse expected = mock(TossBillingKeyResponse.class);
        given(responseSpec.body(TossBillingKeyResponse.class)).willReturn(expected);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        TossBillingKeyResponse actual = client.requestBillingKey("idem-1", "auth-key", "customer-1");

        assertThat(actual).isSameAs(expected);
        RestClient.RequestBodyUriSpec requestSpec = postRequestSpec(restClient);
        verify(requestSpec).uri("/billing/authorizations/issue");
        verify(requestSpec).header("Authorization", basicAuth());
        verify(requestSpec).header("Idempotency-Key", "idem-1");
        verify(requestSpec).contentType(MediaType.APPLICATION_JSON);
        verify(requestSpec).body(java.util.Map.of(
                "authKey", "auth-key",
                "customerKey", "customer-1"
        ));
    }

    @Test
    @DisplayName("빌링키 삭제 성공 시 빌링키 경로와 인증 헤더를 사용한다")
    void deleteBillingKeyUsesBillingKeyPathAndAuthorization() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec requestSpec = stubDeleteResponse(restClient);
        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        client.deleteBillingKeyFromServer("billing-key");

        verify(requestSpec).uri("/billing/{billingKey}", "billing-key");
        verify(requestSpec).header("Authorization", basicAuth());
        verify(requestSpec).retrieve();
    }

    @Test
    @DisplayName("자동 결제 승인 성공 시 빌링키 경로와 요청 본문을 전달하고 응답을 반환한다")
    void requestPaymentReturnsTossResponse() {
        RestClient restClient = mock(RestClient.class);
        RestClient.ResponseSpec responseSpec = stubPostResponse(restClient);
        TossAutomatedPaymentRequest request = mock(TossAutomatedPaymentRequest.class);
        given(request.orderId()).willReturn("order-1");
        given(request.customerKey()).willReturn("customer-1");
        TossAutomatedPaymentResponse expected = mock(TossAutomatedPaymentResponse.class);
        given(responseSpec.body(TossAutomatedPaymentResponse.class)).willReturn(expected);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        TossAutomatedPaymentResponse actual = client.requestPayment("billing-key", request);

        assertThat(actual).isSameAs(expected);
        RestClient.RequestBodyUriSpec requestSpec = postRequestSpec(restClient);
        verify(requestSpec).uri("/billing/{billingKey}", "billing-key");
        verify(requestSpec).header("Authorization", basicAuth());
        verify(requestSpec).contentType(MediaType.APPLICATION_JSON);
        verify(requestSpec).body(request);
    }

    @Test
    @DisplayName("결제 취소 성공 시 결제 취소 경로와 요청 본문을 전달하고 응답을 반환한다")
    void cancelPaymentReturnsTossResponse() {
        RestClient restClient = mock(RestClient.class);
        RestClient.ResponseSpec responseSpec = stubPostResponse(restClient);
        TossCancelRequest request = mock(TossCancelRequest.class);
        TossCancelResponse expected = mock(TossCancelResponse.class);
        given(responseSpec.body(TossCancelResponse.class)).willReturn(expected);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        TossCancelResponse actual = client.cancelPayment("payment-key", request);

        assertThat(actual).isSameAs(expected);
        RestClient.RequestBodyUriSpec requestSpec = postRequestSpec(restClient);
        verify(requestSpec).uri("/payments/{paymentKey}/cancel", "payment-key");
        verify(requestSpec).header("Authorization", basicAuth());
        verify(requestSpec).contentType(MediaType.APPLICATION_JSON);
        verify(requestSpec).body(request);
    }

    @Test
    @DisplayName("정산 조회 성공 시 null 응답을 빈 목록으로 변환한다")
    void fetchSettlementsReturnsEmptyListWhenTossReturnsNull() {
        RestClient restClient = mock(RestClient.class);
        RestClient.ResponseSpec responseSpec = stubGetResponse(restClient);
        given(responseSpec.body(any(ParameterizedTypeReference.class))).willReturn(null);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        assertThat(client.fetchSettlements(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).isEmpty();

        RestClient.RequestHeadersUriSpec requestSpec = getRequestSpec(restClient);
        verify(requestSpec).header("Authorization", basicAuth());
        verify(requestSpec).accept(MediaType.APPLICATION_JSON);
        verify(requestSpec).retrieve();
    }

    @Test
    @DisplayName("주문 ID 결제 조회 성공 시 결제 조회 경로와 인증 헤더를 사용하고 응답을 반환한다")
    void getPaymentByOrderIdReturnsTossResponse() {
        RestClient restClient = mock(RestClient.class);
        RestClient.ResponseSpec responseSpec = stubGetResponse(restClient);
        TossAutomatedPaymentResponse expected = mock(TossAutomatedPaymentResponse.class);
        given(responseSpec.body(TossAutomatedPaymentResponse.class)).willReturn(expected);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        TossAutomatedPaymentResponse actual = client.getPaymentByOrderId("order-1");

        assertThat(actual).isSameAs(expected);
        RestClient.RequestHeadersUriSpec requestSpec = getRequestSpec(restClient);
        verify(requestSpec).uri("/payments/orders/{orderId}", "order-1");
        verify(requestSpec).header("Authorization", basicAuth());
        verify(requestSpec).accept(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("자동 결제 승인 성공 시 토스 응답이 null이면 null을 그대로 반환한다")
    void requestPaymentReturnsNullWhenTossReturnsNull() {
        RestClient restClient = mock(RestClient.class);
        RestClient.ResponseSpec responseSpec = stubPostResponse(restClient);
        given(responseSpec.body(TossAutomatedPaymentResponse.class)).willReturn(null);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        assertThat(client.requestPayment("billing-key", mock(TossAutomatedPaymentRequest.class)))
                .isNull();
    }

    @Test
    @DisplayName("결제 취소 성공 시 토스 응답이 null이면 null을 그대로 반환한다")
    void cancelPaymentReturnsNullWhenTossReturnsNull() {
        RestClient restClient = mock(RestClient.class);
        RestClient.ResponseSpec responseSpec = stubPostResponse(restClient);
        given(responseSpec.body(TossCancelResponse.class)).willReturn(null);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        assertThat(client.cancelPayment("payment-key", mock(TossCancelRequest.class)))
                .isNull();
    }

    @Test
    @DisplayName("정산 조회 성공 시 URI 람다로 기간 쿼리를 만들고 토스 목록을 그대로 반환한다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void fetchSettlementsBuildsPeriodQueryAndReturnsResponseList() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec requestSpec =
                mock(RestClient.RequestHeadersUriSpec.class, RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        List expected = List.of(mock(Object.class));

        given(restClient.get()).willReturn(requestSpec);
        given(requestSpec.uri(any(Function.class))).willAnswer(invocation -> {
            Function<UriBuilder, URI> uriFunction = invocation.getArgument(0);
            URI uri = uriFunction.apply(
                    new DefaultUriBuilderFactory("https://api.tosspayments.com/v1").builder()
            );
            requestedUri.set(uri);
            return requestSpec;
        });
        given(requestSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(any(ParameterizedTypeReference.class))).willReturn(expected);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        List actual = client.fetchSettlements(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        );

        assertThat(actual).isSameAs(expected);
        assertThat(requestedUri.get())
                .hasToString("https://api.tosspayments.com/v1/settlements?startDate=2026-07-01&endDate=2026-07-31");
    }

    @Test
    @DisplayName("주문 ID 결제 조회 성공 시 토스 응답이 null이면 null을 그대로 반환한다")
    void getPaymentByOrderIdReturnsNullWhenTossReturnsNull() {
        RestClient restClient = mock(RestClient.class);
        RestClient.ResponseSpec responseSpec = stubGetResponse(restClient);
        given(responseSpec.body(TossAutomatedPaymentResponse.class)).willReturn(null);

        TossPaymentsClient client = client(restClient, mock(TossErrorHandler.class));

        assertThat(client.getPaymentByOrderId("order-1")).isNull();
    }

    private RestClient.ResponseSpec stubPostResponse(RestClient restClient) {
        RestClient.RequestBodyUriSpec requestSpec = mock(RestClient.RequestBodyUriSpec.class, RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        given(restClient.post()).willReturn(requestSpec);
        given(requestSpec.retrieve()).willReturn(responseSpec);
        return responseSpec;
    }

    private RestClient.RequestBodyUriSpec postRequestSpec(RestClient restClient) {
        return (RestClient.RequestBodyUriSpec) restClient.post();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private RestClient.RequestHeadersUriSpec stubDeleteResponse(RestClient restClient) {
        RestClient.RequestHeadersUriSpec requestSpec =
                mock(RestClient.RequestHeadersUriSpec.class, RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.delete()).willReturn(requestSpec);
        given(requestSpec.retrieve()).willReturn(responseSpec);

        return requestSpec;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private RestClient.ResponseSpec stubGetResponse(RestClient restClient) {
        RestClient.RequestHeadersUriSpec requestSpec =
                mock(RestClient.RequestHeadersUriSpec.class, RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.get()).willReturn(requestSpec);
        given(requestSpec.retrieve()).willReturn(responseSpec);

        return responseSpec;
    }

    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec getRequestSpec(RestClient restClient) {
        return restClient.get();
    }

    private String basicAuth() {
        return "Basic " + java.util.Base64.getEncoder()
                .encodeToString("test-secret-key:".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private TossPaymentsClient client() {
        return client(mock(RestClient.class), mock(TossErrorHandler.class));
    }

    private TossPaymentsClient client(RestClient restClient, TossErrorHandler tossErrorHandler) {
        return new TossPaymentsClient(
                "test-secret-key",
                tossErrorHandler,
                restClient
        );
    }
}
