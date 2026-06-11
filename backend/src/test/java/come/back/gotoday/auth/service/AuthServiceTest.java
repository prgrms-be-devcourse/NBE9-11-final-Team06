package come.back.gotoday.auth.service;

import come.back.gotoday.auth.dto.LoginRequest;
import come.back.gotoday.auth.dto.LoginResponse;
import come.back.gotoday.auth.jwt.JwtTokenProvider;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("로그인에 성공하면 Access Token을 반환한다")
    void login_success() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        Member member = createActiveMember();
        ReflectionTestUtils.setField(member, "id", 1L);

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.password(), member.getPassword())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(member)).thenReturn("access-token");

        // when
        LoginResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.member().email()).isEqualTo("test@example.com");
        assertThat(response.member().nickname()).isEqualTo("낄낄");
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
    }

    @Test
    @DisplayName("로그아웃에 성공한다")
    void logout_success() {
        // given
        Long memberId = 1L;
        Member member = createActiveMember();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when
        authService.logout(memberId);

        // then
        verify(memberRepository).findById(memberId);
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
