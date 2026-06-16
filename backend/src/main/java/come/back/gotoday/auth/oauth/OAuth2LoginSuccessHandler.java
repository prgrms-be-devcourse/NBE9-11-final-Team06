package come.back.gotoday.auth.oauth;

import come.back.gotoday.auth.entity.RefreshToken;
import come.back.gotoday.auth.jwt.JwtTokenProvider;
import come.back.gotoday.auth.repository.RefreshTokenRepository;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
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
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        log.info("OAuth2 로그인 성공 처리 시작");

        try {
            if (!(authentication.getPrincipal() instanceof CustomOAuth2User oAuth2User)) {
                log.warn("OAuth2 로그인 실패: 인증 Principal 타입이 올바르지 않습니다.");
                response.sendRedirect(oAuth2RedirectProperties.failureUrl());
                return;
            }

            Member member = memberRepository.findById(oAuth2User.getMemberId())
                    .orElseThrow(() -> {
                        log.warn("OAuth2 로그인 실패: 회원을 찾을 수 없습니다. memberId={}", oAuth2User.getMemberId());
                        return new IllegalStateException("OAuth2 회원을 찾을 수 없습니다.");
                    });

            if (member.isDeleted()) {
                log.warn("OAuth2 로그인 실패: 탈퇴한 회원입니다. memberId={}", member.getId());
                response.sendRedirect(oAuth2RedirectProperties.failureUrl());
                return;
            }

            String accessToken = jwtTokenProvider.createAccessToken(member);
            String refreshToken = jwtTokenProvider.createRefreshToken(member);

            saveOrUpdateRefreshToken(member, refreshToken);

            addTokenCookie(
                    response,
                    ACCESS_TOKEN_COOKIE_NAME,
                    accessToken,
                    accessTokenExpirationMillis
            );

            addTokenCookie(
                    response,
                    REFRESH_TOKEN_COOKIE_NAME,
                    refreshToken,
                    refreshTokenExpirationMillis
            );

            clearAuthenticationAttributes(request);

            log.info("OAuth2 로그인 성공 처리 완료: memberId={}", member.getId());

            response.sendRedirect(oAuth2RedirectProperties.successUrl());
        } catch (RuntimeException exception) {
            log.warn("OAuth2 로그인 성공 처리 중 오류 발생: message={}", exception.getMessage());
            response.sendRedirect(oAuth2RedirectProperties.failureUrl());
        }
    }

    private void saveOrUpdateRefreshToken(Member member, String refreshToken) {
        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresentOrElse(
                        savedToken -> savedToken.updateToken(
                                refreshToken,
                                jwtTokenProvider.getRefreshTokenExpiresAt()
                        ),
                        () -> refreshTokenRepository.save(
                                RefreshToken.create(
                                        member,
                                        refreshToken,
                                        jwtTokenProvider.getRefreshTokenExpiresAt()
                                )
                        )
                );
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