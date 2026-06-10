package come.back.gotoday.auth.dto;

import come.back.gotoday.member.dto.MemberResponse;
import come.back.gotoday.member.entity.Member;

public record LoginResponse(
        String accessToken,
        String tokenType,
        MemberResponse member
) {

    public static LoginResponse of(String accessToken, Member member) {
        return new LoginResponse(
                accessToken,
                "Bearer",
                MemberResponse.from(member)
        );
    }
}