package come.back.gotoday.external.toss;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.billing.dto.TossBillingKeyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;
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
    public TossBillingKeyResponse requestBillingKey(String authKey, String customerKey) {
        log.info("토스페이먼츠 빌링키 발급 API 호출 시작: customerKey={}", customerKey);

        try {
            String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

            TossBillingKeyResponse response = restClient.post()
                    .uri("/billing/authorizations/issue")
                    .header("Authorization", "Basic " + encodedKey)
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
    public TossBillingKeyResponse recover(ResourceAccessException e, String authKey, String customerKey) {
        log.error("토스페이먼츠 빌링키 발급 API 최대 재시도 횟수 초과 (최종 실패): customerKey={}, message={}", customerKey, e.getMessage(), e);
        // 프로젝트 컨벤션 방식: 실패 로그 및 알림 배치용 예외 상신
        throw new BusinessException(ErrorCode.NETWORK_ERROR_FINAL_FAILED);
    }

    /**
     * 비즈니스 예외 발생 시 원래 에러를 밖으로 그대로 던져주는 복구 핸들러
     */
    @Recover
    public TossBillingKeyResponse recover(BusinessException e, String authKey, String customerKey) {
        log.warn("토스페이먼츠 빌링키 발급 API BusinessException 복구 처리: customerKey={}, message={}", customerKey, e.getMessage());
        throw e;
    }
}