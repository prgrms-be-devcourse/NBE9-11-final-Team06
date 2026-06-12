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
import tools.jackson.databind.JsonNode;
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
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(10));

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
        log.info("서울시 문화행사 API 호출 시작: startIndex={}, endIndex={}", startIndex, endIndex);
        try {
            // 1. 요청 및 응답 수신
            String responseBody = restClient.get()
                    .uri("/{key}/json/culturalEventInfo/{start}/{end}", apiKey, startIndex, endIndex)
                    .retrieve()
                    .body(String.class);
            log.info("서울시 문화행사 API 응답 수신 완료: startIndex={}, endIndex={}, hasBody={}", startIndex, endIndex, StringUtils.hasText(responseBody));

            // 2. 응답 본문이 비어있는 경우 예외 처리
            if (!StringUtils.hasText(responseBody)) {
                log.error("서울시 문화행사 API 응답 본문이 비어 있습니다. startIndex={}, endIndex={}", startIndex, endIndex);
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
            }

            // 1. JSON을 JsonNode 트리 구조로 한 번만 파싱
            JsonNode rootNode = objectMapper.readTree(responseBody);
            log.debug("서울시 문화행사 API 응답 JSON 파싱 완료: startIndex={}, endIndex={}", startIndex, endIndex);

            // 2. 서울시 자체 에러 구조("RESULT") 처리
            if (rootNode.has("RESULT")) {
                SeoulResultResponse errorResponse = objectMapper.treeToValue(rootNode, SeoulResultResponse.class);

                if (errorResponse.isNoData()) {
                    log.warn("서울시 문화행사 API 조회 결과 없음: startIndex={}, endIndex={}, code={}", startIndex, endIndex, errorResponse.result().code());
                    // 호출하는 곳에서 NullPointerException이 나지 않도록 빈 객체 반환
                    return SeoulEventResponse.empty();
                }

                if (errorResponse.isError()) {
                    log.error(
                            "서울시 문화행사 API 비즈니스 에러 발생: startIndex={}, endIndex={}, code={}, message={}",
                            startIndex,
                            endIndex,
                            errorResponse.result().code(),
                            errorResponse.result().message()
                    );
                    throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
                }
            }

            // 3. 정상 DTO 파싱 (이미 파싱된 rootNode를 재사용하므로 효율적)
            SeoulEventResponse response = objectMapper.treeToValue(rootNode, SeoulEventResponse.class);
            int rowCount = Optional.ofNullable(response)
                    .map(SeoulEventResponse::culturalEventInfo)
                    .map(SeoulEventResponse.CulturalEventInfo::row)
                    .map(java.util.List::size)
                    .orElse(0);
            log.info("서울시 문화행사 API 호출 완료: startIndex={}, endIndex={}, rowCount={}", startIndex, endIndex, rowCount);
            return response;

        } catch (ResourceAccessException e) {
            // 타임아웃 및 네트워크 연결 실패 (서버가 응답을 안 줄 때)
            // 이 예외가 발생하면 @Retryable이 작동.
            log.error("서울시 문화행사 API 서버 타임아웃 또는 네트워크 연결 실패: startIndex={}, endIndex={}, message={}", startIndex, endIndex, e.getMessage(), e);
            throw e;

        } catch (RestClientResponseException e) {
            // HTTP 상태 코드가 4xx, 5xx 에러일 때 (서버가 에러 코드를 응답했을 때)
            log.error("서울시 문화행사 API HTTP 에러 발생: startIndex={}, endIndex={}, statusCode={}, responseBody={}", startIndex, endIndex, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR );

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            // JSON 파싱 에러 등 기타 알 수 없는 예외
            log.error("서울시 문화행사 API 데이터 처리 중 오류 발생: startIndex={}, endIndex={}, message={}", startIndex, endIndex, e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    @Recover
    public SeoulEventResponse recover(ResourceAccessException e, int startIndex, int endIndex) {
        log.error("서울시 문화행사 API 최대 재시도 횟수 초과: startIndex={}, endIndex={}, message={}", startIndex, endIndex, e.getMessage(), e);
        //관리자에게 배치가 실패되었다는 로그 혹은 알림을 날릴 수 있게 만든다.
        throw new BusinessException(ErrorCode.EXTERNAL_API_TIMEOUT);
    }
    @Recover
    public SeoulEventResponse recover(BusinessException e, int startIndex, int endIndex) {
        log.warn("서울시 문화행사 API BusinessException 복구 처리: startIndex={}, endIndex={}, message={}", startIndex, endIndex, e.getMessage());
        // 리커버리 레이어에서 래핑을 벗겨내고 원래 예외를 그대로 밖으로 던져줌
        throw e;
    }
}