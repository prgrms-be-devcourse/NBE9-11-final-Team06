package come.back.gotoday.global.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiKeyCheckConfig {

    @Value("${external.seoul.api-key:dummy-key}")
    private String seoulApiKey;

    @Value("${naver.search.client-id:dummy-client-id}")
    private String naverClientId;

    @Value("${naver.search.client-secret:dummy-client-secret}")
    private String naverClientSecret;

    @Value("${jwt.secret:dummy}")
    private String jwtSecret;

    @PostConstruct
    void checkApiKeys() {
        log.info("[ENV CHECK] SEOUL_API_KEY loaded: {}", isRealValue(seoulApiKey, "dummy-key"));
        log.info("[ENV CHECK] NAVER_SEARCH_CLIENT_ID loaded: {}", isRealValue(naverClientId, "dummy-client-id"));
        log.info("[ENV CHECK] NAVER_SEARCH_CLIENT_SECRET loaded: {}", isRealValue(naverClientSecret, "dummy-client-secret"));
        log.info("[ENV CHECK] JWT_SECRET loaded: {}", isRealValue(jwtSecret, "dummy"));
    }

    private boolean isRealValue(String value, String dummyValue) {
        return value != null && !value.isBlank() && !value.equals(dummyValue);
    }
}