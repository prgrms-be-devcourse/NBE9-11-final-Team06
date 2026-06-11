package come.back.gotoday.member.dto;

import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(

        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해야 합니다.")
        String nickname,

        @Size(max = 500, message = "프로필 이미지 URL은 500자 이하로 입력해야 합니다.")
        String profileImageUrl
) {
}