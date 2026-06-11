package come.back.gotoday.auth.service;

import come.back.gotoday.auth.dto.LoginRequest;
import come.back.gotoday.auth.dto.LoginResponse;
import come.back.gotoday.auth.entity.RefreshToken;
import come.back.gotoday.auth.jwt.JwtTokenProvider;
import come.back.gotoday.auth.repository.RefreshTokenRepository;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenLoginResult login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));

        validateActiveMember(member);
        validatePassword(request.password(), member.getPassword());

        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member);

        saveOrUpdateRefreshToken(member, refreshToken);

        return new TokenLoginResult(
                accessToken,
                refreshToken,
                LoginResponse.from(member)
        );
    }

    @Transactional
    public TokenReissueResult reissue(String refreshToken) {
        validateRefreshToken(refreshToken);

        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));

        if (savedRefreshToken.isExpired()) {
            refreshTokenRepository.delete(savedRefreshToken);
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        Member member = savedRefreshToken.getMember();

        validateActiveMember(member);

        String newAccessToken = jwtTokenProvider.createAccessToken(member);

        return new TokenReissueResult(newAccessToken);
    }

    @Transactional
    public void logout(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        validateActiveMember(member);

        refreshTokenRepository.deleteByMemberId(memberId);
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

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }
    }

    private void validateActiveMember(Member member) {
        if (member.isDeleted()) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }
    }

    public record TokenLoginResult(
            String accessToken,
            String refreshToken,
            LoginResponse response
    ) {
    }

    public record TokenReissueResult(
            String accessToken
    ) {
    }
}