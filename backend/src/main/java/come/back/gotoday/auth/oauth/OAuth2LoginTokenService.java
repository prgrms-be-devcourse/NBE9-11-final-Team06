package come.back.gotoday.auth.oauth;

import come.back.gotoday.auth.entity.RefreshToken;
import come.back.gotoday.auth.jwt.JwtTokenProvider;
import come.back.gotoday.auth.repository.RefreshTokenRepository;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2LoginTokenService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public OAuth2LoginTokenResult issueTokens(Long memberId) {
        log.info("OAuth2 JWT 토큰 발급 처리 시작: memberId={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("OAuth2 JWT 토큰 발급 실패: 회원을 찾을 수 없습니다. memberId={}", memberId);
                    return new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
                });

        validateActiveMember(member);

        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member);

        saveOrUpdateRefreshToken(member, refreshToken);

        log.info("OAuth2 JWT 토큰 발급 처리 완료: memberId={}", member.getId());

        return new OAuth2LoginTokenResult(
                accessToken,
                refreshToken,
                member.getId()
        );
    }

    private void validateActiveMember(Member member) {
        if (member.isDeleted()) {
            log.warn("OAuth2 JWT 토큰 발급 실패: 탈퇴한 회원입니다. memberId={}", member.getId());
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }
    }

    private void saveOrUpdateRefreshToken(Member member, String refreshToken) {
        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresentOrElse(
                        savedToken -> {
                            savedToken.updateToken(
                                    refreshToken,
                                    jwtTokenProvider.getRefreshTokenExpiresAt()
                            );
                            log.info("OAuth2 기존 Refresh Token 갱신 완료: memberId={}", member.getId());
                        },
                        () -> {
                            refreshTokenRepository.save(
                                    RefreshToken.create(
                                            member,
                                            refreshToken,
                                            jwtTokenProvider.getRefreshTokenExpiresAt()
                                    )
                            );
                            log.info("OAuth2 신규 Refresh Token 저장 완료: memberId={}", member.getId());
                        }
                );
    }

    public record OAuth2LoginTokenResult(
            String accessToken,
            String refreshToken,
            Long memberId
    ) {
    }
}
