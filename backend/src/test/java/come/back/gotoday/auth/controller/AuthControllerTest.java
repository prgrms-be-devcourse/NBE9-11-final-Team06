package come.back.gotoday.auth.controller;

import come.back.gotoday.auth.dto.LoginRequest;
import come.back.gotoday.auth.dto.LoginResponse;
import come.back.gotoday.auth.jwt.JwtTokenProvider;
import come.back.gotoday.auth.jwt.TokenCookieProvider;
import come.back.gotoday.auth.service.AuthService;
import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenCookieProvider tokenCookieProvider;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("로그인에 성공하면 Access Token과 Refresh Token 쿠키를 응답한다")
    void login_success() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        LoginResponse loginResponse = null;

        AuthService.TokenLoginResult loginResult = new AuthService.TokenLoginResult(
                "access-token",
                "refresh-token",
                loginResponse
        );

        Long accessTokenExpiration = 3_600_000L;
        Long refreshTokenExpiration = 1_209_600_000L;

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "access-token")
                .httpOnly(true)
                .path("/")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "refresh-token")
                .httpOnly(true)
                .path("/")
                .build();

        when(authService.login(request)).thenReturn(loginResult);
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(accessTokenExpiration);
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(refreshTokenExpiration);
        when(tokenCookieProvider.createAccessTokenCookie("access-token", accessTokenExpiration))
                .thenReturn(accessTokenCookie);
        when(tokenCookieProvider.createRefreshTokenCookie("refresh-token", refreshTokenExpiration))
                .thenReturn(refreshTokenCookie);

        // when
        ResponseEntity<ApiResponse<LoginResponse>> response = authController.login(request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ApiResponse<LoginResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("로그인에 성공했습니다.");

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();
        assertThat(cookies).containsExactly(
                accessTokenCookie.toString(),
                refreshTokenCookie.toString()
        );

        verify(authService).login(request);
        verify(jwtTokenProvider).getAccessTokenExpiration();
        verify(jwtTokenProvider).getRefreshTokenExpiration();
        verify(tokenCookieProvider).createAccessTokenCookie("access-token", accessTokenExpiration);
        verify(tokenCookieProvider).createRefreshTokenCookie("refresh-token", refreshTokenExpiration);
    }

    @Test
    @DisplayName("Refresh Token으로 Access Token을 재발급한다")
    void reissue_success() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        String refreshToken = "refresh-token";
        String newAccessToken = "new-access-token";

        AuthService.TokenReissueResult reissueResult = new AuthService.TokenReissueResult(
                newAccessToken
        );

        Long accessTokenExpiration = 3_600_000L;

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", newAccessToken)
                .httpOnly(true)
                .path("/")
                .build();

        when(tokenCookieProvider.resolveRefreshToken(request)).thenReturn(refreshToken);
        when(authService.reissue(refreshToken)).thenReturn(reissueResult);
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(accessTokenExpiration);
        when(tokenCookieProvider.createAccessTokenCookie(newAccessToken, accessTokenExpiration))
                .thenReturn(accessTokenCookie);

        // when
        ResponseEntity<ApiResponse<Void>> response = authController.reissue(request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("Access Token이 재발급되었습니다.");

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();
        assertThat(cookies).containsExactly(accessTokenCookie.toString());

        verify(tokenCookieProvider).resolveRefreshToken(request);
        verify(authService).reissue(refreshToken);
        verify(jwtTokenProvider).getAccessTokenExpiration();
        verify(tokenCookieProvider).createAccessTokenCookie(newAccessToken, accessTokenExpiration);
    }

    @Test
    @DisplayName("로그아웃에 성공하면 Refresh Token을 삭제하고 토큰 쿠키와 세션 쿠키를 제거한다")
    void logout_success() {
        // given
        Long memberId = 1L;

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);

        ResponseCookie deleteAccessTokenCookie = ResponseCookie.from("accessToken", "")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie deleteRefreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie deleteSessionCookie = ResponseCookie.from("JSESSIONID", "")
                .path("/")
                .maxAge(0)
                .build();

        when(userDetails.getMemberId()).thenReturn(memberId);
        doNothing().when(authService).logout(memberId);
        when(tokenCookieProvider.deleteAccessTokenCookie()).thenReturn(deleteAccessTokenCookie);
        when(tokenCookieProvider.deleteRefreshTokenCookie()).thenReturn(deleteRefreshTokenCookie);
        when(tokenCookieProvider.deleteSessionCookie()).thenReturn(deleteSessionCookie);

        // when
        ResponseEntity<ApiResponse<Void>> response = authController.logout(userDetails, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("로그아웃에 성공했습니다.");

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();
        assertThat(cookies).containsExactly(
                deleteAccessTokenCookie.toString(),
                deleteRefreshTokenCookie.toString(),
                deleteSessionCookie.toString()
        );

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(userDetails).getMemberId();
        verify(authService).logout(memberId);
        verify(tokenCookieProvider).deleteAccessTokenCookie();
        verify(tokenCookieProvider).deleteRefreshTokenCookie();
        verify(tokenCookieProvider).deleteSessionCookie();
    }
}