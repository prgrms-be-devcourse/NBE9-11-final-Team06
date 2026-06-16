package come.back.gotoday.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final OAuth2LoginTokenService oAuth2LoginTokenService;
    private final OAuth2RedirectProperties oAuth2RedirectProperties;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMillis;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMillis;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        log.info("OAuth2 로그인 성공 후처리 시작");

        try {
            if (!(authentication.getPrincipal() instanceof CustomOAuth2User oAuth2User)) {
                log.warn("OAuth2 로그인 실패: 인증 Principal 타입이 올바르지 않습니다.");
                response.sendRedirect(oAuth2RedirectProperties.failureUrl());
                return;
            }

            OAuth2LoginTokenService.OAuth2LoginTokenResult tokenResult =
                    oAuth2LoginTokenService.issueTokens(oAuth2User.getMemberId());

            addTokenCookie(
                    response,
                    ACCESS_TOKEN_COOKIE_NAME,
                    tokenResult.accessToken(),
                    accessTokenExpirationMillis
            );

            addTokenCookie(
                    response,
                    REFRESH_TOKEN_COOKIE_NAME,
                    tokenResult.refreshToken(),
                    refreshTokenExpirationMillis
            );

            clearAuthenticationAttributes(request);

            log.info("OAuth2 로그인 성공 후처리 완료: memberId={}", tokenResult.memberId());

            response.sendRedirect(oAuth2RedirectProperties.successUrl());
        } catch (RuntimeException exception) {
            log.warn("OAuth2 로그인 성공 후처리 실패: message={}", exception.getMessage());
            response.sendRedirect(oAuth2RedirectProperties.failureUrl());
        }
    }

    private void addTokenCookie(
            HttpServletResponse response,
            String name,
            String value,
            long maxAgeMillis
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMillis))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
}