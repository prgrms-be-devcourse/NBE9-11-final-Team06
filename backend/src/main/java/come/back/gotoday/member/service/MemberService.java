package come.back.gotoday.member.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.dto.MemberCreateRequest;
import come.back.gotoday.member.dto.MemberResponse;
import come.back.gotoday.member.dto.MemberUpdateRequest;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String DEFAULT_STATUS = "ACTIVE";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse createMember(MemberCreateRequest request) {
        validateDuplicateEmail(request.email());
        validateDuplicateNickname(request.nickname());

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.create(
                request.email(),
                encodedPassword,
                request.nickname(),
                DEFAULT_ROLE,
                DEFAULT_STATUS
        );

        Member savedMember = memberRepository.save(member);

        return MemberResponse.from(savedMember);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        validateActiveMember(member);

        return MemberResponse.from(member);
    }

    @Transactional
    public MemberResponse updateMyInfo(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        validateActiveMember(member);
        validateNicknameForUpdate(member, request.nickname());

        String nickname = Objects.requireNonNullElse(request.nickname(), member.getNickname());
        String profileImageUrl = Objects.requireNonNullElse(request.profileImageUrl(), member.getProfileImageUrl());

        member.updateProfile(nickname, profileImageUrl);

        return MemberResponse.from(member);
    }

    @Transactional
    public void withdrawMyAccount(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        validateActiveMember(member);

        member.withdraw();
    }

    private void validateDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private void validateNicknameForUpdate(Member member, String newNickname) {
        if (newNickname == null || newNickname.equals(member.getNickname())) {
            return;
        }

        validateDuplicateNickname(newNickname);
    }

    private void validateActiveMember(Member member) {
        if (member.isDeleted()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }
}