package come.back.gotoday.member.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.dto.MemberCreateRequest;
import come.back.gotoday.member.dto.MemberResponse;
import come.back.gotoday.member.dto.MemberUpdateRequest;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String DEFAULT_STATUS = "ACTIVE";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse createMember(MemberCreateRequest request) {
        log.info("회원가입 처리 시작: email={}, nickname={}", request.email(), request.nickname());
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
        log.info("회원가입 처리 완료: memberId={}", savedMember.getId());

        return MemberResponse.from(savedMember);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(Long memberId) {
        log.info("내 정보 조회 처리 시작: memberId={}", memberId);
        Member member = findActiveMember(memberId);

        MemberResponse response = MemberResponse.from(member);
        log.info("내 정보 조회 처리 완료: memberId={}", memberId);
        return response;
    }

    @Transactional
    public MemberResponse updateMyInfo(Long memberId, MemberUpdateRequest request) {
        log.info("내 정보 수정 처리 시작: memberId={}", memberId);
        Member member = findActiveMember(memberId);

        validateNicknameForUpdate(member, request.nickname());

        String nickname = request.nickname() != null
                ? request.nickname()
                : member.getNickname();

        String profileImageUrl = request.profileImageUrl() != null
                ? request.profileImageUrl()
                : member.getProfileImageUrl();

        member.updateProfile(nickname, profileImageUrl);

        MemberResponse response = MemberResponse.from(member);
        log.info("내 정보 수정 처리 완료: memberId={}", memberId);
        return response;
    }

    @Transactional
    public void withdrawMyAccount(Long memberId) {
        log.info("회원 탈퇴 처리 시작: memberId={}", memberId);
        Member member = findActiveMember(memberId);

        member.withdraw();
        log.info("회원 탈퇴 처리 완료: memberId={}", memberId);
    }

    private Member findActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("회원 조회 실패: 존재하지 않는 회원입니다. memberId={}", memberId);
                    return new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
                });

        validateActiveMember(member);

        return member;
    }

    private void validateDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            log.warn("회원가입 실패: 중복 이메일입니다. email={}", email);
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            log.warn("회원 처리 실패: 중복 닉네임입니다. nickname={}", nickname);
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
            log.warn("회원 조회 실패: 탈퇴한 회원입니다. memberId={}", member.getId());
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }
}