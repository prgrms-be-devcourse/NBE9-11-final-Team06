package come.back.gotoday.auth.controller;

import come.back.gotoday.auth.dto.LoginRequest;
import come.back.gotoday.auth.dto.LoginResponse;
import come.back.gotoday.auth.jwt.JwtTokenProvider;
import come.back.gotoday.auth.jwt.TokenCookieProvider;
import come.back.gotoday.auth.service.AuthService;
import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenCookieProvider tokenCookieProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request
    ) {
        AuthService.TokenLoginResult result = authService.login(request);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        tokenCookieProvider.createAccessTokenCookie(
                                result.accessToken(),
                                jwtTokenProvider.getAccessTokenExpiration()
                        ).toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        tokenCookieProvider.createRefreshTokenCookie(
                                result.refreshToken(),
                                jwtTokenProvider.getRefreshTokenExpiration()
                        ).toString()
                )
                .body(ApiResponse.success(result.response(), "로그인에 성공했습니다."));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Void>> reissue(HttpServletRequest request) {
        String refreshToken = tokenCookieProvider.resolveRefreshToken(request);

        AuthService.TokenReissueResult result = authService.reissue(refreshToken);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        tokenCookieProvider.createAccessTokenCookie(
                                result.accessToken(),
                                jwtTokenProvider.getAccessTokenExpiration()
                        ).toString()
                )
                .body(ApiResponse.success("Access Token이 재발급되었습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        authService.logout(userDetails.getMemberId());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookieProvider.deleteAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, tokenCookieProvider.deleteRefreshTokenCookie().toString())
                .body(ApiResponse.success("로그아웃에 성공했습니다."));
    }
}