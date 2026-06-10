package come.back.gotoday.auth.controller;

import come.back.gotoday.auth.dto.LoginRequest;
import come.back.gotoday.auth.dto.LoginResponse;
import come.back.gotoday.auth.service.AuthService;
import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success(response, "로그인에 성공했습니다."));
    }
    // 인증 테스트용 엔드포인트, 추후 삭제 예정
    @GetMapping("/me-test")
    public ResponseEntity<ApiResponse<String>> meTest(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("memberId=" + userDetails.getMemberId(), "인증에 성공했습니다.")
        );
    }
}