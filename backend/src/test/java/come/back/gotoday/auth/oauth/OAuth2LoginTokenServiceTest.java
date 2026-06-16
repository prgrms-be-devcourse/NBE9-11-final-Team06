package come.back.gotoday.auth.oauth;

import come.back.gotoday.auth.entity.RefreshToken;
import come.back.gotoday.auth.jwt.JwtTokenProvider;
import come.back.gotoday.auth.repository.RefreshTokenRepository;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginTokenServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private OAuth2LoginTokenService oAuth2LoginTokenService;

    @Test
    @DisplayName("OAuth 로그인 토큰 발급 시 기존 RefreshToken이 없으면 새로 저장한다")
    void issueTokens_createRefreshToken_whenNotExists() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId);

        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(14);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(jwtTokenProvider.createAccessToken(member)).thenReturn(accessToken);
        when(jwtTokenProvider.createRefreshToken(member)).thenReturn(refreshToken);
        when(jwtTokenProvider.getRefreshTokenExpiresAt()).thenReturn(refreshTokenExpiresAt);
        when(refreshTokenRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

        // when
        OAuth2LoginTokenService.OAuth2LoginTokenResult result =
                oAuth2LoginTokenService.issueTokens(memberId);

        // then
        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(refreshToken);
        assertThat(result.memberId()).isEqualTo(memberId);

        ArgumentCaptor<RefreshToken> refreshTokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        verify(refreshTokenRepository).findByMemberId(memberId);

        RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();

        assertThat(savedRefreshToken.getMember()).isEqualTo(member);
        assertThat(savedRefreshToken.getToken()).isEqualTo(refreshToken);
        assertThat(savedRefreshToken.getExpiresAt()).isEqualTo(refreshTokenExpiresAt);
    }

    @Test
    @DisplayName("OAuth 로그인 토큰 발급 시 기존 RefreshToken이 있으면 토큰 값을 갱신한다")
    void issueTokens_updateRefreshToken_whenExists() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId);

        String accessToken = "new-access-token";
        String refreshToken = "new-refresh-token";
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(14);

        RefreshToken savedRefreshToken = RefreshToken.create(
                member,
                "old-refresh-token",
                LocalDateTime.now().plusDays(7)
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(jwtTokenProvider.createAccessToken(member)).thenReturn(accessToken);
        when(jwtTokenProvider.createRefreshToken(member)).thenReturn(refreshToken);
        when(jwtTokenProvider.getRefreshTokenExpiresAt()).thenReturn(refreshTokenExpiresAt);
        when(refreshTokenRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(savedRefreshToken));

        // when
        OAuth2LoginTokenService.OAuth2LoginTokenResult result =
                oAuth2LoginTokenService.issueTokens(memberId);

        // then
        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(refreshToken);
        assertThat(result.memberId()).isEqualTo(memberId);

        assertThat(savedRefreshToken.getToken()).isEqualTo(refreshToken);
        assertThat(savedRefreshToken.getExpiresAt()).isEqualTo(refreshTokenExpiresAt);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("OAuth 로그인 토큰 발급 시 회원이 없으면 MEMBER_NOT_FOUND 예외가 발생한다")
    void issueTokens_throwException_whenMemberNotFound() {
        // given
        Long memberId = 1L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> oAuth2LoginTokenService.issueTokens(memberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("OAuth 로그인 토큰 발급 시 탈퇴한 회원이면 INVALID_LOGIN 예외가 발생한다")
    void issueTokens_throwException_whenMemberDeleted() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId);
        member.withdraw();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> oAuth2LoginTokenService.issueTokens(memberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_LOGIN);

        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(jwtTokenProvider);
    }

    private Member createMember(Long memberId) {
        Member member = Member.create(
                "test@example.com",
                "encoded-password",
                "테스터",
                "USER",
                "ACTIVE"
        );

        ReflectionTestUtils.setField(member, "id", memberId);

        return member;
    }
}