package come.back.gotoday.admin.dto.response;

import come.back.gotoday.member.entity.Member;

import java.time.LocalDateTime;

public record AdminMemberResponse(
        Long memberId,
        String email,
        String nickname,
        String profileImageUrl,
        String role,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminMemberResponse from(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}