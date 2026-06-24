package come.back.gotoday.external.naver.config;

import come.back.gotoday.external.naver.NaverGeocodingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NaverGeocodingProperties.class)
public class NaverGeocodingConfig {
}
