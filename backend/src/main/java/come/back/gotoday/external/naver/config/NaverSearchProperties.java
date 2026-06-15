package come.back.gotoday.external.naver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "naver.search")
public record NaverSearchProperties(
        String clientId,
        String clientSecret,
        String baseUrl
) {
}
