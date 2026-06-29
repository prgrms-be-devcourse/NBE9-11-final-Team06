package come.back.gotoday.global.exception;

import come.back.gotoday.global.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BusinessException 발생 시 ErrorCode 기반 공통 에러 응답을 반환한다")
    void handleBusinessException() {
        // given
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        BusinessException exception = new BusinessException(errorCode);

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(errorCode.getStatus());

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.data()).isNull();
        assertThat(body.code()).isEqualTo(errorCode.getCode());
        assertThat(body.message()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("요청 Body 검증 실패 시 첫 번째 FieldError 메시지를 반환한다")
    void handleValidationException() {
        // given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError = new FieldError(
                "memberCreateRequest",
                "email",
                "이메일은 필수입니다."
        );

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleValidationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_REQUEST.getStatus());

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.data()).isNull();
        assertThat(body.code()).isEqualTo(ErrorCode.INVALID_REQUEST.getCode());
        assertThat(body.message()).isEqualTo("이메일은 필수입니다.");
    }

    @Test
    @DisplayName("요청 Body 검증 실패 메시지가 없으면 기본 INVALID_REQUEST 메시지를 반환한다")
    void handleValidationExceptionWithoutFieldError() {
        // given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleValidationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_REQUEST.getStatus());

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.data()).isNull();
        assertThat(body.code()).isEqualTo(ErrorCode.INVALID_REQUEST.getCode());
        assertThat(body.message()).isEqualTo(ErrorCode.INVALID_REQUEST.getMessage());
    }

    @Test
    @DisplayName("요청 파라미터 검증 실패 시 첫 번째 ConstraintViolation 메시지를 반환한다")
    void handleConstraintViolationException() {
        // given
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("요청 파라미터가 올바르지 않습니다.");

        ConstraintViolationException exception =
                new ConstraintViolationException(Set.of(violation));

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleConstraintViolationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_REQUEST.getStatus());

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.data()).isNull();
        assertThat(body.code()).isEqualTo(ErrorCode.INVALID_REQUEST.getCode());
        assertThat(body.message()).isEqualTo("요청 파라미터가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("요청 파라미터 검증 실패 메시지가 없으면 기본 INVALID_REQUEST 메시지를 반환한다")
    void handleConstraintViolationExceptionWithoutViolation() {
        // given
        ConstraintViolationException exception =
                new ConstraintViolationException(Set.of());

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleConstraintViolationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_REQUEST.getStatus());

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.data()).isNull();
        assertThat(body.code()).isEqualTo(ErrorCode.INVALID_REQUEST.getCode());
        assertThat(body.message()).isEqualTo(ErrorCode.INVALID_REQUEST.getMessage());
    }
}