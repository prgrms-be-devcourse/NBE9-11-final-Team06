package come.back.gotoday.auth.jwt;

import come.back.gotoday.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET =
            "gotoday-local-jwt-secret-key-must-be-at-least-32-bytes";

    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 1209600000L;

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            SECRET,
            ACCESS_TOKEN_EXPIRATION,
            REFRESH_TOKEN_EXPIRATION
    );

    @Test
    @DisplayName("Access Token을 생성한다")
    void createAccessToken_success() {
        // given
        Member member = createMember();

        // when
        String accessToken = jwtTokenProvider.createAccessToken(member);

        // then
        assertThat(accessToken).isNotBlank();
    }

    @Test
    @DisplayName("Refresh Token을 생성한다")
    void createRefreshToken_success() {
        // given
        Member member = createMember();

        // when
        String refreshToken = jwtTokenProvider.createRefreshToken(member);

        // then
        assertThat(refreshToken).isNotBlank();
    }

    @Test
    @DisplayName("유효한 Access Token이면 검증에 성공한다")
    void validateToken_accessToken_success() {
        // given
        Member member = createMember();
        String accessToken = jwtTokenProvider.createAccessToken(member);

        // when
        boolean result = jwtTokenProvider.validateToken(accessToken);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("유효한 Refresh Token이면 검증에 성공한다")
    void validateToken_refreshToken_success() {
        // given
        Member member = createMember();
        String refreshToken = jwtTokenProvider.createRefreshToken(member);

        // when
        boolean result = jwtTokenProvider.validateToken(refreshToken);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Access Token 타입을 확인한다")
    void isAccessToken_success() {
        // given
        Member member = createMember();
        String accessToken = jwtTokenProvider.createAccessToken(member);

        // when
        boolean result = jwtTokenProvider.isAccessToken(accessToken);

        // then
        assertThat(result).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(accessToken)).isFalse();
    }

    @Test
    @DisplayName("Refresh Token 타입을 확인한다")
    void isRefreshToken_success() {
        // given
        Member member = createMember();
        String refreshToken = jwtTokenProvider.createRefreshToken(member);

        // when
        boolean result = jwtTokenProvider.isRefreshToken(refreshToken);

        // then
        assertThat(result).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(refreshToken)).isFalse();
    }

    @Test
    @DisplayName("토큰에서 회원 ID를 추출한다")
    void getMemberId_success() {
        // given
        Member member = createMember();
        String accessToken = jwtTokenProvider.createAccessToken(member);

        // when
        Long memberId = jwtTokenProvider.getMemberId(accessToken);

        // then
        assertThat(memberId).isEqualTo(1L);
    }

    @Test
    @DisplayName("잘못된 토큰이면 검증에 실패한다")
    void validateToken_invalidToken_fail() {
        // given
        String invalidToken = "invalid.token.value";

        // when
        boolean result = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Access Token 만료 시간을 반환한다")
    void getAccessTokenExpiration_success() {
        // when
        long expiration = jwtTokenProvider.getAccessTokenExpiration();

        // then
        assertThat(expiration).isEqualTo(ACCESS_TOKEN_EXPIRATION);
    }

    @Test
    @DisplayName("Refresh Token 만료 시간을 반환한다")
    void getRefreshTokenExpiration_success() {
        // when
        long expiration = jwtTokenProvider.getRefreshTokenExpiration();

        // then
        assertThat(expiration).isEqualTo(REFRESH_TOKEN_EXPIRATION);
    }

    @Test
    @DisplayName("Refresh Token 만료 일시를 반환한다")
    void getRefreshTokenExpiresAt_success() {
        // when
        LocalDateTime expiresAt = jwtTokenProvider.getRefreshTokenExpiresAt();

        // then
        assertThat(expiresAt).isAfter(LocalDateTime.now());
    }

    private Member createMember() {
        Member member = Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "ACTIVE"
        );

        ReflectionTestUtils.setField(member, "id", 1L);

        return member;
    }
}