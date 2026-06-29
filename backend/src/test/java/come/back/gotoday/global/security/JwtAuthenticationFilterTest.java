package come.back.gotoday.global.security;

import come.back.gotoday.auth.jwt.JwtTokenProvider;
import come.back.gotoday.auth.jwt.TokenCookieProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenCookieProvider tokenCookieProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("JWT 토큰이 없으면 인증하지 않고 다음 필터로 넘긴다")
    void doFilterInternalWithoutToken() throws Exception {
        // given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                tokenCookieProvider,
                customUserDetailsService
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenCookieProvider.resolveAccessToken(request)).thenReturn(null);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, never()).parseAndValidateToken(anyString());
        verify(customUserDetailsService, never()).loadUserByMemberId(null);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더에 유효한 Access Token이 있으면 인증 객체를 SecurityContext에 저장한다")
    void doFilterInternalWithValidAuthorizationHeaderToken() throws Exception {
        // given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                tokenCookieProvider,
                customUserDetailsService
        );

        String token = "valid-access-token";
        Long memberId = 1L;

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(tokenCookieProvider.resolveAccessToken(request)).thenReturn(null);
        when(jwtTokenProvider.parseAndValidateToken(token)).thenReturn(claims);
        when(jwtTokenProvider.isAccessToken(claims)).thenReturn(true);
        when(jwtTokenProvider.getMemberId(claims)).thenReturn(memberId);
        when(customUserDetailsService.loadUserByMemberId(memberId)).thenReturn(userDetails);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .when(userDetails)
                .getAuthorities();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(UsernamePasswordAuthenticationToken.class);

        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        verify(jwtTokenProvider).parseAndValidateToken(token);
        verify(jwtTokenProvider).isAccessToken(claims);
        verify(jwtTokenProvider).getMemberId(claims);
        verify(customUserDetailsService).loadUserByMemberId(memberId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("쿠키 토큰과 Authorization 헤더가 모두 있으면 쿠키 토큰을 우선 사용한다")
    void doFilterInternalPreferCookieToken() throws Exception {
        // given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                tokenCookieProvider,
                customUserDetailsService
        );

        String cookieToken = "cookie-access-token";
        String headerToken = "header-access-token";
        Long memberId = 1L;

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer " + headerToken);

        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(tokenCookieProvider.resolveAccessToken(request)).thenReturn(cookieToken);
        when(jwtTokenProvider.parseAndValidateToken(cookieToken)).thenReturn(claims);
        when(jwtTokenProvider.isAccessToken(claims)).thenReturn(true);
        when(jwtTokenProvider.getMemberId(claims)).thenReturn(memberId);
        when(customUserDetailsService.loadUserByMemberId(memberId)).thenReturn(userDetails);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .when(userDetails)
                .getAuthorities();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();

        verify(jwtTokenProvider).parseAndValidateToken(cookieToken);
        verify(jwtTokenProvider, never()).parseAndValidateToken(headerToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Access Token이 아니면 인증 객체를 저장하지 않는다")
    void doFilterInternalWithNonAccessToken() throws Exception {
        // given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                tokenCookieProvider,
                customUserDetailsService
        );

        String token = "refresh-token";

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);

        when(tokenCookieProvider.resolveAccessToken(request)).thenReturn(null);
        when(jwtTokenProvider.parseAndValidateToken(token)).thenReturn(claims);
        when(jwtTokenProvider.isAccessToken(claims)).thenReturn(false);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider).parseAndValidateToken(token);
        verify(jwtTokenProvider).isAccessToken(claims);
        verify(jwtTokenProvider, never()).getMemberId(claims);
        verify(customUserDetailsService, never()).loadUserByMemberId(null);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 검증 중 예외가 발생하면 SecurityContext를 비우고 다음 필터로 넘긴다")
    void doFilterInternalWithInvalidToken() throws Exception {
        // given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                tokenCookieProvider,
                customUserDetailsService
        );

        String token = "invalid-token";

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenCookieProvider.resolveAccessToken(request)).thenReturn(null);
        when(jwtTokenProvider.parseAndValidateToken(token))
                .thenThrow(new RuntimeException("유효하지 않은 토큰입니다."));

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider).parseAndValidateToken(token);
        verify(customUserDetailsService, never()).loadUserByMemberId(null);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 형식이 아니면 인증하지 않는다")
    void doFilterInternalWithInvalidAuthorizationHeaderFormat() throws Exception {
        // given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                tokenCookieProvider,
                customUserDetailsService
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "InvalidTokenFormat");

        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenCookieProvider.resolveAccessToken(request)).thenReturn(null);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, never()).parseAndValidateToken(anyString());
        verify(customUserDetailsService, never()).loadUserByMemberId(null);
        verify(filterChain).doFilter(request, response);
    }
}