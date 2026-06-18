package come.back.gotoday.external.seoul.api_client;

import come.back.gotoday.external.seoul.dto.SeoulEventResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@SpringBootTest(properties = {
        "SEOUL_CROWD_AREA_NAMES=강남역,홍대 관광특구,성수카페거리",
        "crowd.scheduler.enabled=false"
})
public class SeoulEventApiClientRetryTest {

    @Autowired
    private SeoulEventApiClient seoulEventApiClient;

    @MockitoBean
    private RestClient restClient;
    private RestClient.ResponseSpec responseSpec; // 인스턴스 변수로 상량화하면 편리합니다.

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seoulEventApiClient, "restClient", restClient);
    }

    @Test
    @DisplayName("API 호출이 2번 실패하더라도 3번째에 성공하면 예외 없이 정상 DTO를 반환한다")
    void 서울시_API_간헐적_실패_후_성공_테스트() {
        // given
        int startIndex = 1;
        int endIndex = 5;
        String mockSuccessJson = "{\"culturalEventInfo\": {\"list_total_count\": 5, \"RESULT\": {\"CODE\": \"INFO-000\", \"MESSAGE\": \"정상\"}, \"row\": []}}";

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString(), anyString(), anyInt(), anyInt())).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.retrieve()).willReturn(responseSpec);

        // 연이은 3번의 호출에 대한 행동 설정
        given(responseSpec.body(String.class))
                .willThrow(new ResourceAccessException("1번째 네트워크 에러"))
                .willThrow(new ResourceAccessException("2번째 네트워크 에러"))
                .willReturn(mockSuccessJson);

        // when
        SeoulEventResponse response = seoulEventApiClient.fetchEvents(startIndex, endIndex);

        // then
        assertThat(response).isNotNull();

        // 이제 필드가 완전히 교체되었으므로 상호작용 검증(verify)이 성공합니다!
        verify(responseSpec, times(3)).body(String.class);
    }

    @Test
    @DisplayName("API 호출이 3번 모두 실패하면 리커버리 로직이 작동하여 EXTERNAL_API_TIMEOUT 예외를 던진다")
    void 서울시_API_3번_모두_실패_시_리커버리_동작_테스트() {
        // given
        int startIndex = 1;
        int endIndex = 5;

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString(), anyString(), anyInt(), anyInt())).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.retrieve()).willReturn(responseSpec);

        // 핵심 설정: 3번의 호출 모두 네트워크 에러(타임아웃 등)를 뱉도록 설정
        given(responseSpec.body(String.class))
                .willThrow(new ResourceAccessException("1번째 네트워크 에러"))
                .willThrow(new ResourceAccessException("2번째 네트워크 에러"))
                .willThrow(new ResourceAccessException("3번째 네트워크 에러"));

        // when & then
        // 3번 모두 실패했으므로 최종적으로 @Recover 메서드가 실행되어 EXTERNAL_API_TIMEOUT 예외가 터져야 함
        assertThatThrownBy(() -> seoulEventApiClient.fetchEvents(startIndex, endIndex))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    // 리커버리 메서드에서 던지는 타임아웃 에러 코드가 맞는지 검증
                    org.assertj.core.api.Assertions.assertThat(businessException.getErrorCode())
                            .isEqualTo(ErrorCode.EXTERNAL_API_TIMEOUT);
                });

        // 중요: 실제로 최대 재시도 횟수(maxAttempts = 3)만큼 정확히 3번 찔러봤는지 검증
        verify(responseSpec, times(3)).body(String.class);
    }
}
