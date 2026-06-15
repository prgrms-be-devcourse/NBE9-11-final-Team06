package come.back.gotoday.auth.oauth;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("sub"));
    }

    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        return email == null ? null : String.valueOf(email);
    }

    @Override
    public String getNickname() {
        Object name = attributes.get("name");
        return name == null ? "구글사용자" : String.valueOf(name);
    }

    @Override
    public String getProfileImageUrl() {
        Object picture = attributes.get("picture");
        return picture == null ? null : String.valueOf(picture);
    }
}