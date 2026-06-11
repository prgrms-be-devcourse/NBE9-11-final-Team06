package come.back.gotoday.member.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.dto.MemberCreateRequest;
import come.back.gotoday.member.dto.MemberResponse;
import come.back.gotoday.member.dto.MemberUpdateRequest;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원가입에 성공한다")
    void createMember_success() {
        // given
        MemberCreateRequest request = new MemberCreateRequest(
                "test@example.com",
                "password123",
                "낄낄"
        );

        String encodedPassword = "encodedPassword";

        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(memberRepository.existsByNickname(request.nickname())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        MemberResponse response = memberService.createMember(request);

        // then
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.nickname()).isEqualTo("낄낄");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.status()).isEqualTo("ACTIVE");

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());

        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getEmail()).isEqualTo("test@example.com");
        assertThat(savedMember.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedMember.getNickname()).isEqualTo("낄낄");
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 회원가입에 실패한다")
    void createMember_duplicateEmail_fail() {
        // given
        MemberCreateRequest request = new MemberCreateRequest(
                "test@example.com",
                "password123",
                "낄낄"
        );

        when(memberRepository.existsByEmail(request.email())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.createMember(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.DUPLICATE_EMAIL.getMessage());

        verify(memberRepository, never()).save(any(Member.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 회원가입에 실패한다")
    void createMember_duplicateNickname_fail() {
        // given
        MemberCreateRequest request = new MemberCreateRequest(
                "test@example.com",
                "password123",
                "낄낄"
        );

        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(memberRepository.existsByNickname(request.nickname())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.createMember(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.DUPLICATE_NICKNAME.getMessage());

        verify(memberRepository, never()).save(any(Member.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
    @Test
    @DisplayName("내 회원 정보를 조회한다")
    void getMyInfo_success() {
        // given
        Long memberId = 1L;

        Member member = Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "ACTIVE"
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.getMyInfo(memberId);

        // then
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.nickname()).isEqualTo("낄낄");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 내 정보 조회에 실패한다")
    void getMyInfo_notFoundMember_fail() {
        // given
        Long memberId = 1L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMyInfo(memberId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("탈퇴 회원이면 내 정보 조회에 실패한다")
    void getMyInfo_deletedMember_fail() {
        // given
        Long memberId = 1L;

        Member member = Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "DELETED"
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> memberService.getMyInfo(memberId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
    @Test
    @DisplayName("내 회원 정보를 수정한다")
    void updateMyInfo_success() {
        // given
        Long memberId = 1L;

        Member member = Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "ACTIVE"
        );

        MemberUpdateRequest request = new MemberUpdateRequest(
                "낄낄수정",
                "https://example.com/profile.png"
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname(request.nickname())).thenReturn(false);

        // when
        MemberResponse response = memberService.updateMyInfo(memberId, request);

        // then
        assertThat(response.nickname()).isEqualTo("낄낄수정");
        assertThat(response.profileImageUrl()).isEqualTo("https://example.com/profile.png");
    }

    @Test
    @DisplayName("기존 닉네임과 같은 닉네임이면 중복 검사를 하지 않고 수정한다")
    void updateMyInfo_sameNickname_success() {
        // given
        Long memberId = 1L;

        Member member = Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "ACTIVE"
        );

        MemberUpdateRequest request = new MemberUpdateRequest(
                "낄낄",
                "https://example.com/profile.png"
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.updateMyInfo(memberId, request);

        // then
        assertThat(response.nickname()).isEqualTo("낄낄");
        assertThat(response.profileImageUrl()).isEqualTo("https://example.com/profile.png");
        verify(memberRepository, never()).existsByNickname(anyString());
    }

    @Test
    @DisplayName("닉네임을 입력하지 않으면 기존 닉네임을 유지한다")
    void updateMyInfo_nullNickname_success() {
        // given
        Long memberId = 1L;

        Member member = Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "ACTIVE"
        );

        MemberUpdateRequest request = new MemberUpdateRequest(
                null,
                "https://example.com/profile.png"
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.updateMyInfo(memberId, request);

        // then
        assertThat(response.nickname()).isEqualTo("낄낄");
        assertThat(response.profileImageUrl()).isEqualTo("https://example.com/profile.png");
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 회원 정보 수정에 실패한다")
    void updateMyInfo_duplicateNickname_fail() {
        // given
        Long memberId = 1L;

        Member member = Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "ACTIVE"
        );

        MemberUpdateRequest request = new MemberUpdateRequest(
                "중복닉네임",
                "https://example.com/profile.png"
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname(request.nickname())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.updateMyInfo(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.DUPLICATE_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 회원 정보 수정에 실패한다")
    void updateMyInfo_notFoundMember_fail() {
        // given
        Long memberId = 1L;

        MemberUpdateRequest request = new MemberUpdateRequest(
                "낄낄수정",
                "https://example.com/profile.png"
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.updateMyInfo(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("탈퇴 회원이면 회원 정보 수정에 실패한다")
    void updateMyInfo_deletedMember_fail() {
        // given
        Long memberId = 1L;

        Member member = Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "DELETED"
        );

        MemberUpdateRequest request = new MemberUpdateRequest(
                "낄낄수정",
                "https://example.com/profile.png"
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> memberService.updateMyInfo(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
    @Test
    @DisplayName("회원 탈퇴에 성공한다")
    void withdrawMyAccount_success() {
        // given
        Long memberId = 1L;

        Member member = Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "ACTIVE"
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when
        memberService.withdrawMyAccount(memberId);

        // then
        assertThat(member.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 회원 탈퇴에 실패한다")
    void withdrawMyAccount_notFoundMember_fail() {
        // given
        Long memberId = 1L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.withdrawMyAccount(memberId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("이미 탈퇴한 회원이면 회원 탈퇴에 실패한다")
    void withdrawMyAccount_deletedMember_fail() {
        // given
        Long memberId = 1L;

        Member member = Member.create(
                "test@example.com",
                "encodedPassword",
                "낄낄",
                "USER",
                "DELETED"
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> memberService.withdrawMyAccount(memberId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
}