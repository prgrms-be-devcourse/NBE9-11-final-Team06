package come.back.gotoday.external.seoul;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("서울시 혼잡도 API 클라이언트 테스트")
class SeoulCrowdClientTest {

    @Mock
    private SeoulApiProperties seoulApiProperties;

    @Mock
    private ObjectProvider<RestClient.Builder> restClientBuilderProvider;

    @Mock
    private RestClient.Builder restClientBuilder;

    private RestClient restClient;
    private SeoulCrowdClient seoulCrowdClient;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);

        given(restClientBuilderProvider.getIfAvailable(any(Supplier.class)))
                .willReturn(restClientBuilder);
        given(restClientBuilder.build()).willReturn(restClient);
        given(seoulApiProperties.baseUrl())
                .willReturn("http://openapi.seoul.go.kr:8088");
        given(seoulApiProperties.apiKey()).willReturn("test-api-key");

        seoulCrowdClient = new SeoulCrowdClient(
                seoulApiProperties,
                restClientBuilderProvider
        );
    }

    @Test
    @DisplayName("서울시 API의 정상 응답을 반환한다")
    void getCrowdStatusReturnsResponse() {
        String areaName = "성수카페거리";
        SeoulCrowdResponse expectedResponse = mock(SeoulCrowdResponse.class);

        given(restClient.get()
                .uri(any(String.class))
                .retrieve()
                .body(SeoulCrowdResponse.class))
                .willReturn(expectedResponse);

        SeoulCrowdResponse result = seoulCrowdClient.getCrowdStatus(areaName);

        assertThat(result).isSameAs(expectedResponse);
        verify(restClient.get()).uri(
                "http://openapi.seoul.go.kr:8088/test-api-key/json/citydata/1/5/"
                        + "%EC%84%B1%EC%88%98%EC%B9%B4%ED%8E%98%EA%B1%B0%EB%A6%AC"
        );
    }

    @Test
    @DisplayName("서울시 API 응답이 비어 있으면 CROWD_API_RESPONSE_EMPTY 예외가 발생한다")
    void getCrowdStatusThrowsWhenResponseIsNull() {
        given(restClient.get()
                .uri(any(String.class))
                .retrieve()
                .body(SeoulCrowdResponse.class))
                .willReturn(null);

        assertThatThrownBy(() -> seoulCrowdClient.getCrowdStatus("강남역"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CROWD_API_RESPONSE_EMPTY.getMessage());
    }

    @Test
    @DisplayName("서울시 API 호출에 실패하면 EXTERNAL_API_ERROR 예외가 발생한다")
    void getCrowdStatusThrowsWhenRestClientFails() {
        given(restClient.get()
                .uri(any(String.class))
                .retrieve()
                .body(SeoulCrowdResponse.class))
                .willThrow(new RestClientException("서울시 API 호출 실패"));

        assertThatThrownBy(() -> seoulCrowdClient.getCrowdStatus("강남역"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.EXTERNAL_API_ERROR.getMessage());
    }
}
