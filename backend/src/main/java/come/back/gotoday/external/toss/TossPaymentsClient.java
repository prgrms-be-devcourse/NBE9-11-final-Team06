package come.back.gotoday.external.toss;

import come.back.gotoday.external.toss.dto.*;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.billing.dto.TossBillingKeyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TossPaymentsClient {

    private final RestClient restClient;
    private final TossErrorHandler tossErrorHandler;
    private final String secretKey;

    public TossPaymentsClient(
            @Value("${toss.payments.secret-key}") String secretKey,
            TossErrorHandler tossErrorHandler) {
        this.secretKey = secretKey;
        this.tossErrorHandler = tossErrorHandler;

        // 프로젝트 컨벤션: 타임아웃 세팅 (연결 5초, 읽기 10초)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("https://api.tosspayments.com/v1")
                .build();
    }

    @Retryable(
            retryFor = { ResourceAccessException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public TossBillingKeyResponse requestBillingKey(String idempotencyKey,String authKey, String customerKey) {
        log.info("토스페이먼츠 빌링키 발급 API 호출 시작: customerKey={},idempotencyKey={}", customerKey,idempotencyKey);

        try {
            String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

            TossBillingKeyResponse response = restClient.post()
                    .uri("/billing/authorizations/issue")
                    .header("Authorization", "Basic " + encodedKey)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "authKey", authKey,
                            "customerKey", customerKey
                    ))
                    .retrieve()
                    .body(TossBillingKeyResponse.class);

            log.info("토스페이먼츠 빌링키 발급 API 응답 수신 완료: customerKey={}", customerKey);
            return response;

        } catch (ResourceAccessException e) {
            // 프로젝트 컨벤션: 타임아웃 및 네트워크 연결 실패 시 로그 후 에러 전파 (@Retryable 작동 트리거)
            log.error("토스페이먼츠 빌링키 발급 서버 타임아웃 또는 네트워크 연결 실패: customerKey={}, message={}", customerKey, e.getMessage(), e);
            throw e;

        } catch (RestClientResponseException e) {
            // 프로젝트 컨벤션: HTTP 상태 코드가 4xx, 5xx 에러일 때
            log.error("토스페이먼츠 빌링키 발급 HTTP 에러 발생: customerKey={}, statusCode={}, responseBody={}",
                    customerKey, e.getStatusCode(), e.getResponseBodyAsString());

            // 이전에 분리해둔 핸들러를 활용해 내부에서 4xx/5xx 세부 매핑 후 BusinessException을 던집니다.
            tossErrorHandler.handleTossError(e);
            throw e;

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("토스페이먼츠 빌링키 데이터 처리 중 알 수 없는 오류 발생: customerKey={}, message={}", customerKey, e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 최대 재시도 횟수 초과 시 최종 타임아웃 처리 복구 핸들러
     */
    @Recover
    public TossBillingKeyResponse recover(ResourceAccessException e,String idempotencyKey, String authKey, String customerKey) {
        log.error("토스페이먼츠 빌링키 발급 API 최대 재시도 횟수 초과 (최종 실패): customerKey={}, idempotencyKey={}, message={}",
                customerKey, idempotencyKey, e.getMessage(), e);
        // 프로젝트 컨벤션 방식: 실패 로그 및 알림 배치용 예외 상신
        throw new BusinessException(ErrorCode.NETWORK_ERROR_FINAL_FAILED);
    }

    /**
     * 비즈니스 예외 발생 시 원래 에러를 밖으로 그대로 던져주는 복구 핸들러
     */
    @Recover
    public TossBillingKeyResponse recover(BusinessException e, String idempotencyKey, String authKey, String customerKey) {
        log.warn("토스페이먼츠 빌링키 발급 API BusinessException 복구 처리: customerKey={}, idempotencyKey={}, message={}",
                customerKey, idempotencyKey, e.getMessage());
        throw e;
    }


    /**
     * 토스페이먼츠 빌링키 삭제(해지) API 호출 - DELETE 방식
     */
    @Retryable(
            retryFor = { ResourceAccessException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteBillingKeyFromServer(String plainBillingKey) {
        log.info("토스페이먼츠 빌링키 DELETE 삭제 API 호출 시작");
        try {
            String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

            restClient.delete()
                    .uri("/billing/{billingKey}", plainBillingKey)
                    .header("Authorization", "Basic " + encodedKey)
                    .retrieve()
                    .toBodilessEntity(); // 성공 시 200 OK 빈 데이터 처리

            log.info("토스페이먼츠 외부 서버 빌링키 삭제 성공");
        } catch (ResourceAccessException e) {
            log.error("토스페이먼츠 빌링키 삭제 서버 타임아웃 또는 네트워크 연결 실패: message={}", e.getMessage(), e);
            throw e;
        } catch (RestClientResponseException e) {
            log.error("토스페이먼츠 빌링키 삭제 HTTP 에러 발생: statusCode={}, responseBody={}", e.getStatusCode(), e.getResponseBodyAsString());
            tossErrorHandler.handleTossError(e);
            throw e;
        } catch (Exception e) {
            log.error("토스페이먼츠 빌링키 삭제 중 알 수 없는 오류 발생: message={}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 재시도 실패 시 복구 핸들러 메서드명 매칭 수정
     */
    @Recover
    public void recoverDelete(ResourceAccessException e, String plainBillingKey) {
        log.error("토스페이먼츠 빌링키 삭제 API 최대 재시도 횟수 초과 (최종 실패)");
        throw new BusinessException(ErrorCode.NETWORK_ERROR_FINAL_FAILED);
    }

    @Recover
    public void recoverDelete(BusinessException e, String plainBillingKey) {
        throw e;
    }

    /**
     * 토스페이먼츠 빌링키 결제 승인 API 호출 - POST 방식
     */
    @Retryable(
            retryFor = { ResourceAccessException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public TossAutomatedPaymentResponse requestPayment(String plainBillingKey, TossAutomatedPaymentRequest request) {
        log.info("토스페이먼츠 빌링키 결제 승인 API 호출 시작: orderId={}, customerKey={}",
                request.orderId(), request.customerKey());

        try {
            String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

            TossAutomatedPaymentResponse response = restClient.post()
                    .uri("/billing/{billingKey}", plainBillingKey)
                    .header("Authorization", "Basic " + encodedKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TossAutomatedPaymentResponse.class);

            log.info("토스페이먼츠 빌링키 결제 승인 API 완료: orderId={}, status={}",
                    request.orderId(), response != null ? response.status() : "NULL");
            return response;

        } catch (ResourceAccessException e) {
            log.error("토스페이먼츠 결제 승인 서버 타임아웃 또는 네트워크 연결 실패: orderId={}, message={}",
                    request.orderId(), e.getMessage(), e);
            throw e;

        } catch (RestClientResponseException e) {
            log.error("토스페이먼츠 결제 승인 HTTP 에러 발생: orderId={}, statusCode={}, responseBody={}",
                    request.orderId(), e.getStatusCode(), e.getResponseBodyAsString());

            tossErrorHandler.handleTossError(e);
            throw e;

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("토스페이먼츠 결제 승인 데이터 처리 중 알 수 없는 오류 발생: orderId={}, message={}",
                    request.orderId(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 결제 승인 API 최대 재시도 횟수 초과 시 최종 타임아웃 복구 핸들러
     */
    @Recover
    public TossAutomatedPaymentResponse recoverPayment(ResourceAccessException e, String plainBillingKey, TossAutomatedPaymentRequest request) {
        log.error("토스페이먼츠 결제 승인 API 최대 재시도 횟수 초과 (최종 네트워크 실패): orderId={}", request.orderId(), e);
        throw new BusinessException(ErrorCode.NETWORK_ERROR_FINAL_FAILED);
    }

    /**
     * 결제 승인 API BusinessException 발생 시 예외 상신 핸들러
     */
    @Recover
    public TossAutomatedPaymentResponse recoverPayment(BusinessException e, String plainBillingKey, TossAutomatedPaymentRequest request) {
        log.warn("토스페이먼츠 결제 승인 API BusinessException 발생 및 상신 처리: orderId={}, message={}", request.orderId(), e.getMessage());
        throw e;
    }

    /**
     * 토스페이먼츠 결제 취소 API 호출 - POST 방식
     */
    @Retryable(
            retryFor = { ResourceAccessException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public TossCancelResponse cancelPayment(String paymentKey, TossCancelRequest request) {
        log.info("토스페이먼츠 결제 취소 API 호출 시작: paymentKey={}", paymentKey);

        try {
            // 기존과 동일한 시크릿 키 Base64 인증 헤더 구성
            String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

            // curl --request POST https://api.tosspayments.com/v1/payments/{paymentKey}/cancel
            TossCancelResponse response = restClient.post()
                    .uri("/payments/{paymentKey}/cancel", paymentKey)
                    .header("Authorization", "Basic " + encodedKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TossCancelResponse.class);

            log.info("토스페이먼츠 결제 취소 API 완료: paymentKey={}, status={}",
                    paymentKey, response != null ? response.status() : "NULL");
            return response;

        } catch (ResourceAccessException e) {
            log.error("토스페이먼츠 결제 취소 서버 타임아웃 또는 네트워크 연결 실패: paymentKey={}, message={}",
                    paymentKey, e.getMessage(), e);
            throw e;

        } catch (RestClientResponseException e) {
            log.error("토스페이먼츠 결제 취소 HTTP 에러 발생: paymentKey={}, statusCode={}, responseBody={}",
                    paymentKey, e.getStatusCode(), e.getResponseBodyAsString());

            tossErrorHandler.handleTossError(e);
            throw e;

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("토스페이먼츠 결제 취소 데이터 처리 중 알 수 없는 오류 발생: paymentKey={}, message={}",
                    paymentKey, e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 결제 취소 API 최대 재시도 횟수 초과 시 최종 타임아웃 복구 핸들러
     */
    @Recover
    public TossCancelResponse recoverCancel(ResourceAccessException e, String paymentKey, TossCancelRequest request) {
        log.error("토스페이먼츠 결제 취소 API 최대 재시도 횟수 초과 (최종 네트워크 실패): paymentKey={}", paymentKey, e);
        throw new BusinessException(ErrorCode.NETWORK_ERROR_FINAL_FAILED);
    }

    /**
     * 결제 취소 API BusinessException 발생 시 예외 상신 핸들러
     */
    @Recover
    public TossCancelResponse recoverCancel(BusinessException e, String paymentKey, TossCancelRequest request) {
        log.warn("토스페이먼츠 결제 취소 API BusinessException 발생 및 상신 처리: paymentKey={}, message={}", paymentKey, e.getMessage());
        throw e;
    }

    /**
     * 토스페이먼츠 정산 내역 조회 API 호출 - GET 방식
     * 지정된 정산지급일(paidOutDate) 범위를 기준으로 정산 완료 목록을 가져옵니다.
     */
    @Retryable(
            retryFor = { ResourceAccessException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<SettlementDto.TossSettlementResponse> fetchSettlements(LocalDate startDate, LocalDate endDate) {
        log.info("토스페이먼츠 정산 조회 API 호출 시작: startDate={}, endDate={}", startDate, endDate);

        try {
            // 시크릿 키 Base64 인증 헤더 구성
            String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

            // GET /v1/settlements?startDate={startDate}&endDate={endDate}
            List<SettlementDto.TossSettlementResponse> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/settlements")
                            .queryParam("startDate", startDate.toString())
                            .queryParam("endDate", endDate.toString())
                            .build())
                    .header("Authorization", "Basic " + encodedKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<SettlementDto.TossSettlementResponse>>() {});

            log.info("토스페이먼츠 정산 조회 API 완료: 수집된 데이터 개수={}건", response != null ? response.size() : 0);
            return response != null ? response : List.of();

        } catch (ResourceAccessException e) {
            log.error("토스페이먼츠 정산 조회 서버 타임아웃 또는 네트워크 연결 실패: startDate={}, endDate={}, message={}",
                    startDate, endDate, e.getMessage(), e);
            throw e;

        } catch (RestClientResponseException e) {
            log.error("토스페이먼츠 정산 조회 HTTP 에러 발생: statusCode={}, responseBody={}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            tossErrorHandler.handleTossError(e);
            throw e;

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("토스페이먼츠 정산 조회 데이터 처리 중 알 수 없는 오류 발생: message={}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 정산 조회 API 최대 재시도 횟수 초과 시 최종 타임아웃 복구 핸들러
     */
    @Recover
    public List<SettlementDto.TossSettlementResponse> recoverSettlement(ResourceAccessException e, LocalDate startDate, LocalDate endDate) {
        log.error("토스페이먼츠 정산 조회 API 최대 재시도 횟수 초과 (최종 네트워크 실패): startDate={}, endDate={}", startDate, endDate, e);
        throw new BusinessException(ErrorCode.NETWORK_ERROR_FINAL_FAILED);
    }

    /**
     * 정산 조회 API BusinessException 발생 시 예외 상신 핸들러
     */
    @Recover
    public List<SettlementDto.TossSettlementResponse> recoverSettlement(BusinessException e, LocalDate startDate, LocalDate endDate) {
        log.warn("토스페이먼츠 정산 조회 API BusinessException 발생 및 상신 처리: message={}", e.getMessage());
        throw e;
    }

    public TossAutomatedPaymentResponse getPaymentByOrderId(String orderId) {
        log.info("토스페이먼츠 주문 ID 기준 결제 조회 API 호출 시작: orderId={}", orderId);

        try {
            String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

            TossAutomatedPaymentResponse response = restClient.get()
                    .uri("/payments/orders/{orderId}", orderId)
                    .header("Authorization", "Basic " + encodedKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(TossAutomatedPaymentResponse.class);

            log.info("토스페이먼츠 주문 ID 기준 결제 조회 API 완료: orderId={}, status={}",
                    orderId, response != null ? response.status() : "NULL");
            return response;

        } catch (RestClientResponseException e) {
            // 404 Not Found 같은 에러는 아직 토스에 결제가 접수조차 안 되었다는 뜻이므로
            // 호출부에서 제어할 수 있도록 그대로 예외를 던집니다.
            log.warn("토스페이먼츠 결제 조회 HTTP 에러 발생 (결제 미접수 가능성 있음): orderId={}, statusCode={}",
                    orderId, e.getStatusCode());
            throw e;
        } catch (Exception e) {
            log.error("토스페이먼츠 결제 조회 중 알 수 없는 오류 발생: orderId={}, message={}",
                    orderId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }
}