package come.back.gotoday.member.dto;

import come.back.gotoday.member.entity.Member;

public record MemberResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        String role,
        String status
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getRole(),
                member.getStatus()
        );
    }
}