package come.back.gotoday.external.seoul;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.seoul")
public record SeoulApiProperties(
        String baseUrl,
        String apiKey
) {
}
