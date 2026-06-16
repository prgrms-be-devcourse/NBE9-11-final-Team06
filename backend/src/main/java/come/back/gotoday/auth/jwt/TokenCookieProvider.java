package come.back.gotoday.auth.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class TokenCookieProvider {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    public static final String SESSION_COOKIE_NAME = "JSESSIONID";

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    public ResponseCookie createAccessTokenCookie(String accessToken, long expirationMillis) {
        return createCookie(
                ACCESS_TOKEN_COOKIE_NAME,
                accessToken,
                Duration.ofMillis(expirationMillis)
        );
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken, long expirationMillis) {
        return createCookie(
                REFRESH_TOKEN_COOKIE_NAME,
                refreshToken,
                Duration.ofMillis(expirationMillis)
        );
    }

    public ResponseCookie deleteAccessTokenCookie() {
        return deleteCookie(ACCESS_TOKEN_COOKIE_NAME);
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return deleteCookie(REFRESH_TOKEN_COOKIE_NAME);
    }

    public ResponseCookie deleteSessionCookie() {
        return deleteCookie(SESSION_COOKIE_NAME);
    }

    public String resolveAccessToken(HttpServletRequest request) {
        return resolveCookieValue(request, ACCESS_TOKEN_COOKIE_NAME).orElse(null);
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        return resolveCookieValue(request, REFRESH_TOKEN_COOKIE_NAME).orElse(null);
    }

    private ResponseCookie createCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie deleteCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private Optional<String> resolveCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}