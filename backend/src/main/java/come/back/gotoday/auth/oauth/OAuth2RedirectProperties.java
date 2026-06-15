package come.back.gotoday.auth.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth2.redirect")
public record OAuth2RedirectProperties(
        String successUrl,
        String failureUrl
) {
}