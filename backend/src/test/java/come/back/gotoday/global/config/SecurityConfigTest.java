package come.back.gotoday.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    @DisplayName("PasswordEncoder는 BCryptPasswordEncoder를 반환한다")
    void passwordEncoder() {
        // given
        SecurityConfig securityConfig = createSecurityConfig(
                List.of("http://localhost:3000")
        );

        // when
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // then
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("BCryptPasswordEncoder는 비밀번호를 암호화하고 검증할 수 있다")
    void passwordEncoderMatchesRawPassword() {
        // given
        SecurityConfig securityConfig = createSecurityConfig(
                List.of("http://localhost:3000")
        );

        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        String rawPassword = "password1234";

        // when
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // then
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    @DisplayName("CORS 설정을 생성한다")
    void corsConfigurationSource() {
        // given
        List<String> allowedOrigins = List.of(
                "http://localhost:3000",
                "https://gotoday.com"
        );

        SecurityConfig securityConfig = createSecurityConfig(allowedOrigins);

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/members/me");

        // when
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        // then
        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactlyElementsOf(allowedOrigins);
        assertThat(configuration.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders())
                .containsExactly("*");
        assertThat(configuration.getExposedHeaders())
                .containsExactly("Authorization");
        assertThat(configuration.getAllowCredentials())
                .isTrue();
    }

    private SecurityConfig createSecurityConfig(List<String> allowedOrigins) {
        return new SecurityConfig(
                null,
                new CorsProperties(allowedOrigins),
                null,
                null,
                null,
                null
        );
    }
}