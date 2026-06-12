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
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenLoginResult login(LoginRequest request) {
        log.info("로그인 처리 시작: email={}", request.email());
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("로그인 실패: 존재하지 않는 이메일입니다. email={}", request.email());
                    return new BusinessException(ErrorCode.INVALID_LOGIN);
                });

        validateActiveMember(member);
        validatePassword(request.password(), member.getPassword());

        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member);

        saveOrUpdateRefreshToken(member, refreshToken);
        log.info("로그인 처리 완료: memberId={}", member.getId());

        return new TokenLoginResult(
                accessToken,
                refreshToken,
                LoginResponse.from(member)
        );
    }

    @Transactional
    public TokenReissueResult reissue(String refreshToken) {
        log.info("Access Token 재발급 처리 시작");
        if (refreshToken == null) {
            log.warn("Access Token 재발급 실패: Refresh Token이 없습니다.");
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Access Token 재발급 실패: 저장된 Refresh Token을 찾을 수 없습니다.");
                    return new BusinessException(ErrorCode.INVALID_LOGIN);
                });

        if (savedRefreshToken.isExpired()) {
            log.warn("Access Token 재발급 실패: 만료된 Refresh Token입니다. memberId={}", resolveMemberIdSafely(savedRefreshToken));
            refreshTokenRepository.delete(savedRefreshToken);
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        try {
            Claims claims = jwtTokenProvider.parseAndValidateToken(refreshToken);

            if (!jwtTokenProvider.isRefreshToken(claims)) {
                log.warn("Access Token 재발급 실패: Refresh Token 타입이 아닙니다. memberId={}", resolveMemberIdSafely(savedRefreshToken));
                refreshTokenRepository.delete(savedRefreshToken);
                throw new BusinessException(ErrorCode.INVALID_LOGIN);
            }

            Member member = savedRefreshToken.getMember();

            validateActiveMember(member);

            String newAccessToken = jwtTokenProvider.createAccessToken(member);
            log.info("Access Token 재발급 처리 완료: memberId={}", member.getId());

            return new TokenReissueResult(newAccessToken);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Access Token 재발급 실패: Refresh Token 검증 중 오류가 발생했습니다. memberId={}, message={}", resolveMemberIdSafely(savedRefreshToken), exception.getMessage());
            refreshTokenRepository.delete(savedRefreshToken);
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }
    }

    @Transactional
    public void logout(Long memberId) {
        log.info("로그아웃 처리 시작: memberId={}", memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("로그아웃 실패: 존재하지 않는 회원입니다. memberId={}", memberId);
                    return new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
                });

        validateActiveMember(member);

        refreshTokenRepository.deleteByMemberId(memberId);
        log.info("로그아웃 처리 완료: memberId={}", memberId);
    }

    private void saveOrUpdateRefreshToken(Member member, String refreshToken) {
        log.info("Refresh Token 저장 또는 갱신 시작: memberId={}", member.getId());
        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresentOrElse(
                        savedToken -> {
                            savedToken.updateToken(
                                    refreshToken,
                                    jwtTokenProvider.getRefreshTokenExpiresAt()
                            );
                            log.info("기존 Refresh Token 갱신 완료: memberId={}", member.getId());
                        },
                        () -> {
                            refreshTokenRepository.save(
                                    RefreshToken.create(
                                            member,
                                            refreshToken,
                                            jwtTokenProvider.getRefreshTokenExpiresAt()
                                    )
                            );
                            log.info("신규 Refresh Token 저장 완료: memberId={}", member.getId());
                        }
                );
    }

    private Long resolveMemberIdSafely(RefreshToken refreshToken) {
        try {
            if (refreshToken == null || refreshToken.getMember() == null) {
                return null;
            }
            return refreshToken.getMember().getId();
        } catch (RuntimeException exception) {
            log.warn("Refresh Token 회원 ID 조회 실패: message={}", exception.getMessage());
            return null;
        }
    }

    private void validateActiveMember(Member member) {
        if (member.isDeleted()) {
            log.warn("인증 실패: 탈퇴한 회원입니다. memberId={}", member.getId());
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            log.warn("로그인 실패: 비밀번호가 일치하지 않습니다.");
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