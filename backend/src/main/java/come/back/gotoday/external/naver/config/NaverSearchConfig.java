package come.back.gotoday.external.naver.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NaverSearchProperties.class)
public class NaverSearchConfig {

    @Bean
    public RestClient naverSearchRestClient(NaverSearchProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
