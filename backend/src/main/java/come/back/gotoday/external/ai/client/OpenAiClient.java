
package come.back.gotoday.external.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import come.back.gotoday.external.ai.config.OpenAiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiProperties properties;

    public OpenAiClient(OpenAiProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String generateText(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(Map.of(
                                "type", "input_text",
                                "text", prompt
                        ))
                )),
                "max_output_tokens", properties.maxOutputTokens()
        );

        String responseBody = restClient.post()
                .uri("/v1/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractOutputText(responseBody);
    }

    private String extractOutputText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode outputText = root.path("output_text");
            if (outputText.isTextual() && !outputText.asText().isBlank()) {
                return outputText.asText().trim();
            }

            for (JsonNode output : root.path("output")) {
                for (JsonNode content : output.path("content")) {
                    JsonNode text = content.path("text");
                    if (text.isTextual() && !text.asText().isBlank()) {
                        return text.asText().trim();
                    }
                }
            }

            throw new IllegalStateException("OpenAI 응답에서 생성된 텍스트를 찾을 수 없습니다.");
        } catch (Exception exception) {
            throw new IllegalStateException("OpenAI 응답 파싱에 실패했습니다.", exception);
        }
    }
}
