package come.back.gotoday.external.seoul.api_client;

import come.back.gotoday.external.seoul.dto.SeoulEventResponse;
import come.back.gotoday.external.seoul.dto.SeoulResultResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
@Slf4j
public class SeoulEventApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public SeoulEventApiClient(@Value("${seoul.api.key}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("http://openapi.seoul.go.kr:8088")
                .build();
    }
    @Retryable(
            retryFor = { ResourceAccessException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public SeoulEventResponse fetchEvents(int startIndex, int endIndex) {
        try {
            // 1. 요청 및 응답 수신
            String responseBody = restClient.get()
                    .uri("/{key}/json/culturalEventInfo/{start}/{end}", apiKey, startIndex, endIndex)
                    .retrieve()
                    .body(String.class);

            // 2. 응답 본문이 비어있는 경우 예외 처리
            if (!StringUtils.hasText(responseBody)) {
                log.error("서울시 API 응답 본문이 비어 있습니다.");
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
            }

            // 3. 서울시 자체 에러 구조("RESULT") 처리
            if (responseBody.contains("\"RESULT\"")) {
                SeoulResultResponse errorResponse = objectMapper.readValue(responseBody, SeoulResultResponse.class);

                if (errorResponse.isNoData()) {
                    log.warn("서울시 API - 조회된 데이터가 없습니다. (INFO-200)");
                    // 호출하는 곳에서 NullPointerException이 나지 않도록 빈 객체 반환
                    return SeoulEventResponse.empty();
                }

                if (errorResponse.isError()) {
                    log.error("서울시 API 비즈니스 에러 발생 - 코드: {}, 메시지: {}",
                            errorResponse.result().code(), errorResponse.result().message());
                    throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
                }
            }

            // 4. 정상 DTO 파싱
            return objectMapper.readValue(responseBody, SeoulEventResponse.class);

        } catch (ResourceAccessException e) {
            // 타임아웃 및 네트워크 연결 실패 (서버가 응답을 안 줄 때)
            // 이 예외가 발생하면 @Retryable이 작동.
            log.error("서울시 API 서버 타임아웃 또는 네트워크 연결 실패: {}", e.getMessage());
            throw e;

        } catch (RestClientResponseException e) {
            // HTTP 상태 코드가 4xx, 5xx 에러일 때 (서버가 에러 코드를 응답했을 때)
            log.error("서울시 API HTTP 에러 발생 - 상태코드: {}, 응답본문: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR );

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            // JSON 파싱 에러 등 기타 알 수 없는 예외
            log.error("API 데이터 처리 중 기타 에러 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    @Recover
    public SeoulEventResponse recover(ResourceAccessException e, int startIndex, int endIndex) {
        log.error("최대 재시도 횟수를 초과했습니다. 서울시 API 서버가 완전히 응답하지 않습니다. (인덱스: {} ~ {})", startIndex, endIndex);
        //관리자에게 배치가 실패되었다는 로그 혹은 알림을 날릴 수 있게 만든다.
        throw new BusinessException(ErrorCode.EXTERNAL_API_TIMEOUT);
    }
    @Recover
    public SeoulEventResponse recover(BusinessException e, int startIndex, int endIndex) {
        // 리커버리 레이어에서 래핑을 벗겨내고 원래 예외를 그대로 밖으로 던져줌
        throw e;
    }
}