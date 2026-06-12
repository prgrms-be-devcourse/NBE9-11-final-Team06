package come.back.gotoday.auth.dto;

import come.back.gotoday.member.dto.MemberResponse;
import come.back.gotoday.member.entity.Member;

public record LoginResponse(
        MemberResponse member
) {
    public static LoginResponse from(Member member) {
        return new LoginResponse(MemberResponse.from(member));
    }
}