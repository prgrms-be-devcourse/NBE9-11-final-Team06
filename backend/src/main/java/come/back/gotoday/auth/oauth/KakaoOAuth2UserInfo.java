package come.back.gotoday.auth.oauth;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    @SuppressWarnings("unchecked")
    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        if (this.kakaoAccount == null) {
            this.profile = null;
        } else {
            this.profile = (Map<String, Object>) this.kakaoAccount.get("profile");
        }
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        if (kakaoAccount == null) {
            return null;
        }

        Object email = kakaoAccount.get("email");
        return email == null ? null : String.valueOf(email);
    }

    @Override
    public String getNickname() {
        if (profile == null) {
            return "카카오사용자";
        }

        Object nickname = profile.get("nickname");
        return nickname == null ? "카카오사용자" : String.valueOf(nickname);
    }

    @Override
    public String getProfileImageUrl() {
        if (profile == null) {
            return null;
        }

        Object imageUrl = profile.get("profile_image_url");
        return imageUrl == null ? null : String.valueOf(imageUrl);
    }
}