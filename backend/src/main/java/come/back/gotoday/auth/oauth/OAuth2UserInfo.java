package come.back.gotoday.auth.oauth;

public interface OAuth2UserInfo {

    String getProviderId();

    String getEmail();

    String getNickname();

    String getProfileImageUrl();
}
