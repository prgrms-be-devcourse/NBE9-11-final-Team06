package come.back.gotoday.global.security;

import come.back.gotoday.auth.jwt.JwtTokenProvider;
import come.back.gotoday.auth.jwt.TokenCookieProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenCookieProvider tokenCookieProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        ResolvedToken resolvedToken = resolveToken(request);

        if (StringUtils.hasText(resolvedToken.token())) {
            authenticate(resolvedToken.token(), resolvedToken.source(), request);
        } else {
            logForCourseApi("JWT 토큰 없음. uri={}, method={}", requestUri, request.getMethod());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, String tokenSource, HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        try {
            logForCourseApi(
                    "JWT 인증 시도. uri={}, method={}, tokenSource={}",
                    requestUri,
                    request.getMethod(),
                    tokenSource
            );

            Claims claims = jwtTokenProvider.parseAndValidateToken(token);

            if (!jwtTokenProvider.isAccessToken(claims)) {
                logForCourseApi(
                        "JWT 인증 실패. access token 아님. uri={}, tokenSource={}",
                        requestUri,
                        tokenSource
                );
                return;
            }

            Long memberId = jwtTokenProvider.getMemberId(claims);

            CustomUserDetails userDetails =
                    (CustomUserDetails) customUserDetailsService.loadUserByMemberId(memberId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            logForCourseApi(
                    "JWT 인증 성공. uri={}, memberId={}, tokenSource={}",
                    requestUri,
                    memberId,
                    tokenSource
            );
        } catch (RuntimeException e) {
            log.warn(
                    "JWT 인증 실패. uri={}, method={}, tokenSource={}, reason={}",
                    requestUri,
                    request.getMethod(),
                    tokenSource,
                    e.getMessage()
            );

            SecurityContextHolder.clearContext();
        }
    }

    private ResolvedToken resolveToken(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        String cookieToken = tokenCookieProvider.resolveAccessToken(request);

        if (StringUtils.hasText(cookieToken)) {
            logForCourseApi("JWT 토큰 출처: cookie. uri={}", requestUri);
            return new ResolvedToken(cookieToken, "cookie");
        }

        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            logForCourseApi("JWT 토큰 출처: Authorization header. uri={}", requestUri);
            return new ResolvedToken(
                    bearerToken.substring(BEARER_PREFIX.length()),
                    "Authorization"
            );
        }

        return new ResolvedToken(null, "none");
    }

    private void logForCourseApi(String message, Object... args) {
        /*
         * 로그가 너무 많이 찍히는 것을 막기 위해 course API만 확인한다.
         * 다른 API까지 확인하고 싶으면 조건을 제거하면 된다.
         */
        if (args.length > 0 && String.valueOf(args[0]).startsWith("/api/courses")) {
            log.info(message, args);
            return;
        }

        /*
         * 위 조건에서 uri가 첫 번째 인자가 아닌 로그도 있으므로 fallback 처리.
         */
        for (Object arg : args) {
            if (arg != null && String.valueOf(arg).startsWith("/api/courses")) {
                log.info(message, args);
                return;
            }
        }
    }

    private record ResolvedToken(
            String token,
            String source
    ) {
    }
}