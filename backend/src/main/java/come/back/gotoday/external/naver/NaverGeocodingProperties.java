
package come.back.gotoday.external.naver;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "naver.geocoding")
public record NaverGeocodingProperties(
        String clientId,
        String clientSecret,
        String baseUrl
) {
}
