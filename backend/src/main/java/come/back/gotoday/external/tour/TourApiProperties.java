package come.back.gotoday.external.tour;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "external.tour")
public class TourApiProperties {

    private String baseUrl;
    private String apiKey;
    private String mobileOs;
    private String mobileApp;
}