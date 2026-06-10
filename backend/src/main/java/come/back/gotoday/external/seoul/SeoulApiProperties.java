package come.back.gotoday.external.seoul;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 서울시 Open API 호출에 필요한 설정 값을 관리하는 클래스입니다.
 *
 * application-local.yml 또는 application.yml의 external.seoul 하위 설정을
 * Spring이 자동으로 읽어와 baseUrl, apiKey 필드에 매핑합니다.
 *
 * 예시 설정:
 * external:
 *   seoul:
 *     base-url: http://openapi.seoul.go.kr:8088
 *     api-key: ${SEOUL_API_KEY}
 */
@ConfigurationProperties(prefix = "external.seoul")
public record SeoulApiProperties(
        /** 서울시 Open API의 기본 URL입니다. */
        String baseUrl,

        /** 서울시 Open API 인증키입니다. 실제 값은 환경변수로 관리합니다. */
        String apiKey
) {

    /**
     * 애플리케이션 구동 시점에 필수 설정값이 누락되었는지 검증합니다.
     *
     * 설정이 잘못된 상태로 서버가 실행되는 것을 막기 위해
     * baseUrl 또는 apiKey가 비어 있으면 바로 예외를 발생시킵니다.
     */
    public SeoulApiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("서울시 API 기본 URL(baseUrl) 설정이 누락되었습니다.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("서울시 API 인증키(apiKey) 설정이 누락되었습니다.");
        }
    }
}
