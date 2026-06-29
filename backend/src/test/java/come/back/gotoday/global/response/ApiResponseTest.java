package come.back.gotoday.global.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("데이터가 있는 성공 응답을 생성한다")
    void successWithData() {
        // given
        Long data = 1L;

        // when
        ApiResponse<Long> response = ApiResponse.success(data);

        // then
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(data);
        assertThat(response.code()).isNull();
        assertThat(response.message()).isNull();
    }

    @Test
    @DisplayName("데이터와 메시지가 있는 성공 응답을 생성한다")
    void successWithDataAndMessage() {
        // given
        Long data = 1L;
        String message = "요청이 성공했습니다.";

        // when
        ApiResponse<Long> response = ApiResponse.success(data, message);

        // then
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(data);
        assertThat(response.code()).isNull();
        assertThat(response.message()).isEqualTo(message);
    }

    @Test
    @DisplayName("메시지만 있는 성공 응답을 생성한다")
    void successWithMessage() {
        // given
        String message = "회원가입이 완료되었습니다.";

        // when
        ApiResponse<Void> response = ApiResponse.success(message);

        // then
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNull();
        assertThat(response.code()).isNull();
        assertThat(response.message()).isEqualTo(message);
    }

    @Test
    @DisplayName("실패 응답을 생성한다")
    void fail() {
        // given
        String code = "INVALID_REQUEST";
        String message = "잘못된 요청입니다.";

        // when
        ApiResponse<Void> response = ApiResponse.fail(code, message);

        // then
        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.code()).isEqualTo(code);
        assertThat(response.message()).isEqualTo(message);
    }
}