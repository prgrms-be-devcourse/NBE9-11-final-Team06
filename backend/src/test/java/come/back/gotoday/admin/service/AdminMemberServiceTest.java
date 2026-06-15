package come.back.gotoday.admin.service;

import come.back.gotoday.admin.dto.response.AdminMemberResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {

    @InjectMocks
    private AdminMemberService adminMemberService;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("관리자는 회원 목록을 페이징 조회할 수 있다")
    void getMembers_success() {
        Pageable pageable = PageRequest.of(0, 10);

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getEmail()).thenReturn("user@test.com");
        when(member.getNickname()).thenReturn("사용자");
        when(member.getProfileImageUrl()).thenReturn(null);
        when(member.getRole()).thenReturn("USER");
        when(member.getStatus()).thenReturn("ACTIVE");

        Page<Member> members = new PageImpl<>(List.of(member), pageable, 1);

        when(memberRepository.findAll(pageable)).thenReturn(members);

        Page<AdminMemberResponse> result = adminMemberService.getMembers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).memberId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).email()).isEqualTo("user@test.com");
        assertThat(result.getContent().get(0).nickname()).isEqualTo("사용자");
        assertThat(result.getContent().get(0).role()).isEqualTo("USER");
        assertThat(result.getContent().get(0).status()).isEqualTo("ACTIVE");

        verify(memberRepository).findAll(pageable);
    }

    @Test
    @DisplayName("관리자는 회원을 탈퇴 처리할 수 있다")
    void deleteMember_success() {
        Long memberId = 1L;
        Member member = mock(Member.class);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(member.isDeleted()).thenReturn(false);

        adminMemberService.deleteMember(memberId);

        verify(member).withdraw();
    }

    @Test
    @DisplayName("이미 탈퇴한 회원은 다시 탈퇴 처리하지 않는다")
    void deleteMember_alreadyDeleted_return() {
        Long memberId = 1L;
        Member member = mock(Member.class);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(member.isDeleted()).thenReturn(true);

        adminMemberService.deleteMember(memberId);

        verify(member, never()).withdraw();
    }

    @Test
    @DisplayName("존재하지 않는 회원을 탈퇴 처리하면 예외가 발생한다")
    void deleteMember_memberNotFound_fail() {
        Long memberId = 999L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMemberService.deleteMember(memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

        verify(memberRepository).findById(memberId);
    }
}