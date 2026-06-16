package come.back.gotoday.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String AUTHORIZATION_REQUEST_BASE_URI = "/oauth2/authorization";
    private static final String GOOGLE_REGISTRATION_ID = "google";
    private static final String KAKAO_REGISTRATION_ID = "kakao";

    private final OAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver;

    public CustomOAuth2AuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        this.defaultAuthorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        AUTHORIZATION_REQUEST_BASE_URI
                );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest =
                defaultAuthorizationRequestResolver.resolve(request);

        String registrationId = resolveRegistrationId(request);

        return customizeAuthorizationRequest(authorizationRequest, registrationId);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request,
            String clientRegistrationId
    ) {
        OAuth2AuthorizationRequest authorizationRequest =
                defaultAuthorizationRequestResolver.resolve(request, clientRegistrationId);

        return customizeAuthorizationRequest(authorizationRequest, clientRegistrationId);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            String registrationId
    ) {
        if (authorizationRequest == null) {
            return null;
        }

        Map<String, Object> additionalParameters = new LinkedHashMap<>(
                authorizationRequest.getAdditionalParameters()
        );

        if (GOOGLE_REGISTRATION_ID.equals(registrationId)) {
            additionalParameters.put("prompt", "select_account");
        }

        if (KAKAO_REGISTRATION_ID.equals(registrationId)) {
            additionalParameters.put("prompt", "login");
        }

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();
    }

    private String resolveRegistrationId(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        int lastSlashIndex = requestUri.lastIndexOf("/");

        if (lastSlashIndex == -1) {
            return "";
        }

        return requestUri.substring(lastSlashIndex + 1);
    }
}