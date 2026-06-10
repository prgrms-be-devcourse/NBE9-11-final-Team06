package come.back.gotoday.auth.controller;

import come.back.gotoday.auth.dto.LoginRequest;
import come.back.gotoday.auth.dto.LoginResponse;
import come.back.gotoday.auth.service.AuthService;
import come.back.gotoday.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}