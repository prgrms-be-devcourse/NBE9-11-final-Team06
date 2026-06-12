package come.back.gotoday.auth.service;

import come.back.gotoday.auth.dto.LoginRequest;
import come.back.gotoday.auth.entity.RefreshToken;
import come.back.gotoday.auth.jwt.JwtTokenProvider;
import come.back.gotoday.auth.repository.RefreshTokenRepository;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Claims claims;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("로그인에 성공하면 Access Token과 Refresh Token을 생성한다")
    void login_success() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        Member member = createActiveMember();
        ReflectionTestUtils.setField(member, "id", 1L);

        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(14);

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.password(), member.getPassword())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(member)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(member)).thenReturn("refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpiresAt()).thenReturn(refreshTokenExpiresAt);
        when(refreshTokenRepository.findByMemberId(member.getId())).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        AuthService.TokenLoginResult result = authService.login(request);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.response().member().email()).isEqualTo("test@example.com");
        assertThat(result.response().member().nickname()).isEqualTo("낄낄");

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();
        assertThat(savedRefreshToken.getMember()).isEqualTo(member);
        assertThat(savedRefreshToken.getToken()).isEqualTo("refresh-token");
        assertThat(savedRefreshToken.getExpiresAt()).isEqualTo(refreshTokenExpiresAt);
    }

    @Test
    @DisplayName("이미 저장된 Refresh Token이 있으면 로그인 시 토큰을 갱신한다")
    void login_existingRefreshToken_update() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        Member member = createActiveMember();
        ReflectionTestUtils.setField(member, "id", 1L);

        RefreshToken savedRefreshToken = RefreshToken.create(
                member,
                "old-refresh-token",
                LocalDateTime.now().plusDays(7)
        );

        LocalDateTime newRefreshTokenExpiresAt = LocalDateTime.now().plusDays(14);

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.password(), member.getPassword())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(member)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(member)).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpiresAt()).thenReturn(newRefreshTokenExpiresAt);
        when(refreshTokenRepository.findByMemberId(member.getId())).thenReturn(Optional.of(savedRefreshToken));

        // when
        AuthService.TokenLoginResult result = authService.login(request);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(savedRefreshToken.getToken()).isEqualTo("new-refresh-token");
        assertThat(savedRefreshToken.getExpiresAt()).isEqualTo(newRefreshTokenExpiresAt);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 로그인에 실패한다")
    void login_notFoundEmail_fail() {
        // given
        LoginRequest request = new LoginRequest("none@example.com", "password123");

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_LOGIN.getMessage());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).createAccessToken(any(Member.class));
        verify(jwtTokenProvider, never()).createRefreshToken(any(Member.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    void login_invalidPassword_fail() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "wrong1234");

        Member member = createActiveMember();

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.password(), member.getPassword())).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_LOGIN.getMessage());

        verify(jwtTokenProvider, never()).createAccessToken(any(Member.class));
        verify(jwtTokenProvider, never()).createRefreshToken(any(Member.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("탈퇴 회원은 로그인에 실패한다")
    void login_deletedMember_fail() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        Member member = createDeletedMember();

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_LOGIN.getMessage());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).createAccessToken(any(Member.class));
        verify(jwtTokenProvider, never()).createRefreshToken(any(Member.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh Token으로 Access Token을 재발급한다")
    void reissue_success() {
        // given
        String refreshToken = "refresh-token";
        String newAccessToken = "new-access-token";

        Member member = createActiveMember();
        ReflectionTestUtils.setField(member, "id", 1L);

        RefreshToken savedRefreshToken = RefreshToken.create(
                member,
                refreshToken,
                LocalDateTime.now().plusDays(14)
        );

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(savedRefreshToken));
        when(jwtTokenProvider.parseAndValidateToken(refreshToken)).thenReturn(claims);
        when(jwtTokenProvider.isRefreshToken(claims)).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(member)).thenReturn(newAccessToken);

        // when
        AuthService.TokenReissueResult result = authService.reissue(refreshToken);

        // then
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
    }

    @Test
    @DisplayName("Refresh Token이 없으면 Access Token 재발급에 실패한다")
    void reissue_nullRefreshToken_fail() {
        // given
        String refreshToken = null;

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_LOGIN.getMessage());

        verify(refreshTokenRepository, never()).findByToken(anyString());
        verify(jwtTokenProvider, never()).parseAndValidateToken(anyString());
    }

    @Test
    @DisplayName("DB에 저장되지 않은 Refresh Token이면 Access Token 재발급에 실패한다")
    void reissue_notSavedRefreshToken_fail() {
        // given
        String refreshToken = "refresh-token";

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_LOGIN.getMessage());

        verify(jwtTokenProvider, never()).parseAndValidateToken(anyString());
    }

    @Test
    @DisplayName("만료된 Refresh Token이면 삭제 후 Access Token 재발급에 실패한다")
    void reissue_expiredRefreshToken_fail() {
        // given
        String refreshToken = "refresh-token";

        Member member = createActiveMember();

        RefreshToken savedRefreshToken = RefreshToken.create(
                member,
                refreshToken,
                LocalDateTime.now().minusDays(1)
        );

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(savedRefreshToken));

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_LOGIN.getMessage());

        verify(refreshTokenRepository).delete(savedRefreshToken);
        verify(jwtTokenProvider, never()).parseAndValidateToken(anyString());
    }

    @Test
    @DisplayName("JWT 파싱에 실패한 Refresh Token이면 삭제 후 Access Token 재발급에 실패한다")
    void reissue_invalidRefreshToken_fail() {
        // given
        String refreshToken = "invalid-refresh-token";

        Member member = createActiveMember();

        RefreshToken savedRefreshToken = RefreshToken.create(
                member,
                refreshToken,
                LocalDateTime.now().plusDays(14)
        );

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(savedRefreshToken));
        when(jwtTokenProvider.parseAndValidateToken(refreshToken))
                .thenThrow(new RuntimeException("Invalid token"));

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_LOGIN.getMessage());

        verify(refreshTokenRepository).delete(savedRefreshToken);
    }

    @Test
    @DisplayName("Refresh Token 타입이 아니면 삭제 후 Access Token 재발급에 실패한다")
    void reissue_notRefreshTokenType_fail() {
        // given
        String refreshToken = "access-token";

        Member member = createActiveMember();

        RefreshToken savedRefreshToken = RefreshToken.create(
                member,
                refreshToken,
                LocalDateTime.now().plusDays(14)
        );

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(savedRefreshToken));
        when(jwtTokenProvider.parseAndValidateToken(refreshToken)).thenReturn(claims);
        when(jwtTokenProvider.isRefreshToken(claims)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_LOGIN.getMessage());

        verify(refreshTokenRepository).delete(savedRefreshToken);
    }

    @Test
    @DisplayName("탈퇴 회원의 Refresh Token이면 Access Token 재발급에 실패한다")
    void reissue_deletedMember_fail() {
        // given
        String refreshToken = "refresh-token";

        Member member = createDeletedMember();

        RefreshToken savedRefreshToken = RefreshToken.create(
                member,
                refreshToken,
                LocalDateTime.now().plusDays(14)
        );

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(savedRefreshToken));
        when(jwtTokenProvider.parseAndValidateToken(refreshToken)).thenReturn(claims);
        when(jwtTokenProvider.isRefreshToken(claims)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_LOGIN.getMessage());
    }

    @Test
    @DisplayName("로그아웃에 성공하면 Refresh Token을 삭제한다")
    void logout_success() {
        // given
        Long memberId = 1L;
        Member member = createActiveMember();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when
        authService.logout(memberId);

        // then
        verify(memberRepository).findById(memberId);
        verify(refreshTokenRepository).deleteByMemberId(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 로그아웃에 실패한다")
    void logout_notFoundMember_fail() {
        // given
        Long memberId = 1L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.logout(memberId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());

        verify(refreshTokenRepository, never()).deleteByMemberId(anyLong());
    }

    @Test
    @DisplayName("탈퇴 회원은 로그아웃에 실패한다")
    void logout_deletedMember_fail() {
        // given
        Long memberId = 1L;
        Member member = createDeletedMember();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> authService.logout(memberId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_LOGIN.getMessage());

        verify(refreshTokenRepository, never()).deleteByMemberId(anyLong());
    }

    private Member createActiveMember() {
        return Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "ACTIVE"
        );
    }

    private Member createDeletedMember() {
        return Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "DELETED"
        );
    }
}