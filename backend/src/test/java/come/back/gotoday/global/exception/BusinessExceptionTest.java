package come.back.gotoday.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    @DisplayName("ErrorCode로 BusinessException을 생성한다")
    void createBusinessExceptionWithErrorCode() {
        // given
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        // when
        BusinessException exception = new BusinessException(errorCode);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
    }
}