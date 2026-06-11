package come.back.gotoday.auth.jwt;

import come.back.gotoday.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET =
            "gotoday-local-jwt-secret-key-must-be-at-least-32-bytes";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            SECRET,
            3600000L
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
    @DisplayName("유효한 토큰이면 검증에 성공한다")
    void validateToken_success() {
        // given
        Member member = createMember();
        String accessToken = jwtTokenProvider.createAccessToken(member);

        // when
        boolean result = jwtTokenProvider.validateToken(accessToken);

        // then
        assertThat(result).isTrue();
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