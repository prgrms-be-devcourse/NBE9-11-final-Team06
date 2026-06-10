package come.back.gotoday.auth.service;

import come.back.gotoday.auth.dto.LoginRequest;
import come.back.gotoday.auth.dto.LoginResponse;
import come.back.gotoday.auth.jwt.JwtTokenProvider;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));

        validateActiveMember(member);
        validatePassword(request.password(), member.getPassword());

        String accessToken = jwtTokenProvider.createAccessToken(member);

        return LoginResponse.of(accessToken, member);
    }

    @Transactional(readOnly = true)
    public void logout(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        validateActiveMember(member);
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
}