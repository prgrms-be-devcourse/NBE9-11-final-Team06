package come.back.gotoday.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    @DisplayName("모든 ErrorCode는 status, code, message를 가진다")
    void allErrorCodesHaveRequiredValues() {
        // when & then
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertThat(errorCode.getStatus())
                    .as("%s status", errorCode.name())
                    .isNotNull();

            assertThat(errorCode.getCode())
                    .as("%s code", errorCode.name())
                    .isNotBlank();

            assertThat(errorCode.getMessage())
                    .as("%s message", errorCode.name())
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("ErrorCode의 code 값은 중복되지 않는다")
    void errorCodeValuesAreUnique() {
        // given
        Map<String, Long> codeCountMap = Arrays.stream(ErrorCode.values())
                .collect(Collectors.groupingBy(
                        ErrorCode::getCode,
                        Collectors.counting()
                ));

        // when
        List<String> duplicateCodes = codeCountMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();

        // then
        assertThat(duplicateCodes).isEmpty();
    }

    @Test
    @DisplayName("ErrorCode의 code 문자열은 enum 이름과 동일하다")
    void errorCodeValueEqualsEnumName() {
        // when & then
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertThat(errorCode.getCode())
                    .as("%s code", errorCode.name())
                    .isEqualTo(errorCode.name());
        }
    }

    @Test
    @DisplayName("회원 관련 ErrorCode의 상태 코드가 올바르다")
    void memberErrorCodesHaveCorrectStatus() {
        assertThat(ErrorCode.DUPLICATE_EMAIL.getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.DUPLICATE_NICKNAME.getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.MEMBER_NOT_FOUND.getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.INVALID_LOGIN.getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("공통 ErrorCode의 상태 코드가 올바르다")
    void commonErrorCodesHaveCorrectStatus() {
        assertThat(ErrorCode.INVALID_REQUEST.getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.EXTERNAL_API_ERROR.getStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(ErrorCode.EXTERNAL_API_TIMEOUT.getStatus())
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("대표 ErrorCode의 code와 message가 올바르다")
    void representativeErrorCodesHaveCorrectCodeAndMessage() {
        assertThat(ErrorCode.INVALID_REQUEST.getCode())
                .isEqualTo("INVALID_REQUEST");
        assertThat(ErrorCode.INVALID_REQUEST.getMessage())
                .isEqualTo("잘못된 요청입니다.");

        assertThat(ErrorCode.DUPLICATE_EMAIL.getCode())
                .isEqualTo("DUPLICATE_EMAIL");
        assertThat(ErrorCode.DUPLICATE_EMAIL.getMessage())
                .isEqualTo("이미 가입된 이메일입니다.");

        assertThat(ErrorCode.MEMBER_NOT_FOUND.getCode())
                .isEqualTo("MEMBER_NOT_FOUND");
        assertThat(ErrorCode.MEMBER_NOT_FOUND.getMessage())
                .isEqualTo("회원을 찾을 수 없습니다.");

        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .isEqualTo("서버 내부 오류가 발생했습니다.");
    }
}