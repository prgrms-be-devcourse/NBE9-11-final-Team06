package come.back.gotoday.external.toss;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class TossResponseParser {

    private final ObjectMapper objectMapper;

    public String parseErrorCode(String errorBody) {
        if (errorBody == null || errorBody.isBlank()) {
            return "UNKNOWN_ERROR";
        }
        try {
            JsonNode root = objectMapper.readTree(errorBody);
            return root.path("code").asString("UNKNOWN_ERROR");
        } catch (Exception e) {
            return "UNKNOWN_ERROR";
        }
    }
}