package come.back.gotoday.external.seoul.api_client;

import come.back.gotoday.external.seoul.dto.SeoulEventResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SeoulEventApiClientTest {

    private SeoulEventApiClient seoulEventApiClient;

    @BeforeEach
    void setUp() {
        seoulEventApiClient = new SeoulEventApiClient("test-api-key", new ObjectMapper());
    }

    @Test
    @DisplayName("서울시 행사 API 클라이언트 생성 테스트")
    void 서울시_행사_API_클라이언트를_생성한다() {
        assertThat(seoulEventApiClient).isNotNull();
    }

    @Test
    @DisplayName("서울시 행사 API 데이터 없음 응답은 빈 문화행사 정보로 표현된다")
    void 데이터_없음_응답은_빈_문화행사_정보를_가진다() {
        SeoulEventResponse response = SeoulEventResponse.empty();

        assertThat(response).isNotNull();
        assertThat(response.culturalEventInfo()).isNotNull();
        assertThat(response.culturalEventInfo().listTotalCount()).isZero();
        assertThat(response.culturalEventInfo().row()).isEmpty();
    }
}