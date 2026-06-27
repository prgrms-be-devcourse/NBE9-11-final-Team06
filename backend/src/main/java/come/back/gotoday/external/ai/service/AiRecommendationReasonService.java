package come.back.gotoday.external.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import come.back.gotoday.external.ai.client.OpenAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiRecommendationReasonService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationReasonService.class);
    private static final String FALLBACK_REASON =
            "선택한 취향, 동행 유형, 출발 위치를 함께 고려해 행사와 관광지를 추천했어요.";
    private static final String PLACE_FALLBACK_REASON =
            "선택한 취향과 출발 위치를 함께 고려해 추천된 장소입니다.";
    private static final String FINAL_COURSE_FALLBACK_REASON =
            "선택한 장소와 방문 순서를 바탕으로 완성한 나들이 코스입니다.";
    private static final String FINAL_PLACE_FALLBACK_REASON =
            "완성된 코스의 방문 순서에 맞춰 구성한 장소입니다.";

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateFinalCourseReason(FinalCourseReasonContext context) {
        try {
            String generatedReason = openAiClient.generateText(buildFinalCoursePrompt(context));
            return normalizeFinalCourseReason(generatedReason);
        } catch (Exception exception) {
            log.warn("AI 최종 코스 추천 이유 생성에 실패했습니다. fallback 문구를 반환합니다.", exception);
            return FINAL_COURSE_FALLBACK_REASON;
        }
    }

    public List<String> generateFinalPlaceReasons(List<FinalPlaceReasonContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return List.of();
        }

        try {
            String generatedJson = openAiClient.generateText(buildFinalPlaceReasonsPrompt(contexts));
            List<String> reasons = parsePlaceReasons(generatedJson, contexts.size());

            if (reasons.size() != contexts.size()) {
                throw new IllegalStateException("AI 최종 장소 추천 이유 개수가 코스 장소 개수와 다릅니다.");
            }

            return reasons.stream()
                    .map(this::normalizeFinalPlaceReason)
                    .toList();
        } catch (Exception exception) {
            log.warn("AI 최종 장소별 추천 이유 생성에 실패했습니다. fallback 문구를 반환합니다.", exception);
            return contexts.stream()
                    .map(context -> FINAL_PLACE_FALLBACK_REASON)
                    .toList();
        }
    }

    public List<String> generatePlaceReasons(List<PlaceReasonContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return List.of();
        }

        try {
            String generatedJson = openAiClient.generateText(buildPlaceReasonsPrompt(contexts));
            List<String> reasons = parsePlaceReasons(generatedJson, contexts.size());

            if (reasons.size() != contexts.size()) {
                throw new IllegalStateException("AI 장소 추천 이유 개수가 후보 장소 개수와 다릅니다.");
            }

            return reasons.stream()
                    .map(this::normalizePlaceReason)
                    .toList();
        } catch (Exception exception) {
            log.warn("AI 장소별 추천 이유 생성에 실패했습니다. fallback 문구를 반환합니다.", exception);
            return contexts.stream()
                    .map(context -> PLACE_FALLBACK_REASON)
                    .toList();
        }
    }

    private String buildPlaceReasonsPrompt(List<PlaceReasonContext> contexts) {
        StringBuilder candidates = new StringBuilder();

        for (int index = 0; index < contexts.size(); index++) {
            PlaceReasonContext context = contexts.get(index);
            String preferences = context.preferenceCategories().isEmpty()
                    ? "없음"
                    : String.join(", ", context.preferenceCategories());

            candidates.append(index + 1)
                    .append(". 장소명: ").append(blankToDefault(context.placeName(), "추천된 장소"))
                    .append(" | 장소 유형: ").append(blankToDefault(context.placeType(), "장소"))
                    .append(" | 카테고리: ").append(blankToDefault(context.categoryName(), "정보 없음"))
                    .append(" | 날짜: ").append(context.visitDate())
                    .append(" | 출발 위치: ").append(blankToDefault(context.departureAreaName(), "선택한 출발 위치"))
                    .append(" | 동행 유형: ").append(blankToDefault(context.companionType(), "선택한 동행 유형"))
                    .append(" | 선호 카테고리: ").append(preferences)
                    .append(System.lineSeparator());
        }

        return """
                당신은 서울 나들이 코스 추천 서비스의 장소별 설명 작성 도우미입니다.
                아래 후보 장소마다 사용자에게 보여 줄 추천 이유를 작성하세요.
                제공된 사실만 사용해야 합니다.

                [후보 장소]
                %s

                [작성 규칙]
                - 입력된 후보 장소 순서와 동일한 순서로 이유를 작성합니다.
                - 각 이유는 한 문장, 45자 이내의 친절한 존댓말로 작성합니다.
                - 제공되지 않은 날씨, 혼잡도, 거리, 이동 시간, 운영 시간, 예약 정보는 언급하지 않습니다.
                - 추천 알고리즘이나 AI라는 단어는 언급하지 않습니다.
                - 반드시 문자열만 담긴 JSON 배열만 출력합니다.
                - 마크다운 코드 블록, 설명, 번호, 제목은 절대 출력하지 않습니다.

                출력 예시:
                ["선호한 역사 문화 분위기를 즐기기 좋은 장소입니다.", "가볍게 둘러보기 좋은 관광지입니다."]
                """.formatted(candidates.toString());
    }

    private String buildFinalCoursePrompt(FinalCourseReasonContext context) {
        String orderedPlaces = context.orderedPlaceNames().stream()
                .filter(placeName -> placeName != null && !placeName.isBlank())
                .collect(Collectors.joining(" → "));

        String preferences = context.preferenceCategories().isEmpty()
                ? "없음"
                : String.join(", ", context.preferenceCategories());

        return """
                당신은 서울 나들이 코스 추천 서비스의 최종 코스 설명 작성 도우미입니다.
                사용자가 실제로 선택했고 방문 순서까지 확정된 장소만 기준으로 코스 추천 이유를 작성하세요.
                후보 단계의 장소나 선택하지 않은 장소는 절대 언급하지 마세요.
                제공된 사실만 사용해야 합니다.

                [최종 코스 조건]
                날짜: %s
                출발 위치: %s
                동행 유형: %s
                선호 카테고리: %s
                확정 방문 순서: %s

                [작성 규칙]
                - 2문장 이내의 친절한 존댓말로 작성합니다.
                - 실제 선택된 장소와 방문 순서를 자연스럽게 설명합니다.
                - 제공되지 않은 날씨, 혼잡도, 거리, 이동 시간, 운영 시간, 예약 정보는 언급하지 않습니다.
                - 추천 알고리즘이나 AI라는 단어는 언급하지 않습니다.
                - 목록, 제목, 마크다운, 따옴표는 출력하지 않습니다.
                """.formatted(
                context.visitDate(),
                blankToDefault(context.departureAreaName(), "선택한 출발 위치"),
                blankToDefault(context.companionType(), "선택한 동행 유형"),
                preferences,
                blankToDefault(orderedPlaces, "선택한 장소")
        );
    }

    private String buildFinalPlaceReasonsPrompt(List<FinalPlaceReasonContext> contexts) {
        StringBuilder places = new StringBuilder();

        for (int index = 0; index < contexts.size(); index++) {
            FinalPlaceReasonContext context = contexts.get(index);
            places.append(index + 1)
                    .append(". 방문 순서: ").append(context.sequence())
                    .append(" | 장소명: ").append(blankToDefault(context.placeName(), "선택한 장소"))
                    .append(" | 장소 유형: ").append(blankToDefault(context.placeType(), "장소"))
                    .append(" | 앞 장소: ").append(blankToDefault(context.previousPlaceName(), "코스 시작"))
                    .append(" | 다음 장소: ").append(blankToDefault(context.nextPlaceName(), "코스 마무리"))
                    .append(System.lineSeparator());
        }

        return """
                당신은 서울 나들이 코스 추천 서비스의 최종 코스 장소 설명 작성 도우미입니다.
                아래 장소들은 사용자가 실제로 선택했고 방문 순서가 확정된 코스입니다.
                각 장소가 이 코스의 해당 순서에 포함된 이유를 작성하세요.
                제공된 사실만 사용해야 합니다.

                [확정 코스 장소]
                %s

                [작성 규칙]
                - 입력된 장소 순서와 동일한 순서로 이유를 작성합니다.
                - 각 이유는 한 문장, 45자 이내의 친절한 존댓말로 작성합니다.
                - 앞 장소와 다음 장소가 제공된 경우에만 동선의 흐름을 자연스럽게 언급할 수 있습니다.
                - 제공되지 않은 날씨, 혼잡도, 거리, 이동 시간, 운영 시간, 예약 정보는 언급하지 않습니다.
                - 추천 알고리즘이나 AI라는 단어는 언급하지 않습니다.
                - 반드시 문자열만 담긴 JSON 배열만 출력합니다.
                - 마크다운 코드 블록, 설명, 번호, 제목은 절대 출력하지 않습니다.
                """.formatted(places.toString());
    }

    private List<String> parsePlaceReasons(String generatedJson, int expectedSize) throws Exception {
        String json = generatedJson
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();

        JsonNode root = objectMapper.readTree(json);
        if (!root.isArray()) {
            throw new IllegalStateException("AI 장소 추천 이유 응답이 JSON 배열이 아닙니다.");
        }

        List<String> reasons = new ArrayList<>();
        for (JsonNode reason : root) {
            if (!reason.isTextual()) {
                throw new IllegalStateException("AI 장소 추천 이유 배열에 문자열이 아닌 값이 포함되어 있습니다.");
            }
            reasons.add(reason.asText());
        }

        if (reasons.size() != expectedSize) {
            throw new IllegalStateException("AI 장소 추천 이유 개수가 후보 장소 개수와 다릅니다.");
        }

        return reasons;
    }

    private String normalizeFinalCourseReason(String reason) {
        String normalizedReason = normalizeReason(reason);
        return FALLBACK_REASON.equals(normalizedReason)
                ? FINAL_COURSE_FALLBACK_REASON
                : normalizedReason;
    }

    private String normalizeFinalPlaceReason(String reason) {
        String normalizedReason = normalizeReason(reason);
        return FALLBACK_REASON.equals(normalizedReason)
                ? FINAL_PLACE_FALLBACK_REASON
                : normalizedReason;
    }

    private String normalizePlaceReason(String reason) {
        String normalizedReason = normalizeReason(reason);
        return FALLBACK_REASON.equals(normalizedReason) ? PLACE_FALLBACK_REASON : normalizedReason;
    }

    public AiRecommendationReasonService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public String generateCourseReason(CourseReasonContext context) {
        try {
            String generatedReason = openAiClient.generateText(buildPrompt(context));
            return normalizeReason(generatedReason);
        } catch (Exception exception) {
            log.warn("AI 코스 추천 이유 생성에 실패했습니다. fallback 문구를 반환합니다.", exception);
            return FALLBACK_REASON;
        }
    }

    private String buildPrompt(CourseReasonContext context) {
        String places = context.placeNames().stream()
                .filter(placeName -> placeName != null && !placeName.isBlank())
                .collect(Collectors.joining(", "));

        String preferences = context.preferenceCategories().isEmpty()
                ? "없음"
                : String.join(", ", context.preferenceCategories());

        return """
                당신은 서울 나들이 코스 추천 서비스의 설명 작성 도우미입니다.
                아래 사실만 사용하여 사용자에게 보여 줄 코스 추천 이유를 한국어로 작성하세요.

                [추천 조건]
                날짜: %s
                출발 위치: %s
                동행 유형: %s
                선호 카테고리: %s
                추천 장소: %s

                [작성 규칙]
                - 2문장 이내로 작성합니다.
                - 친절한 존댓말을 사용합니다.
                - 제공되지 않은 날씨, 혼잡도, 거리, 이동 시간, 예약 정보는 언급하지 않습니다.
                - 장소명 외의 목록, 제목, 마크다운, 따옴표는 출력하지 않습니다.
                - 추천 알고리즘이나 AI라는 단어는 언급하지 않습니다。
                """.formatted(
                context.visitDate(),
                blankToDefault(context.departureAreaName(), "선택한 출발 위치"),
                blankToDefault(context.companionType(), "선택한 동행 유형"),
                preferences,
                blankToDefault(places, "추천된 장소")
        );
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return FALLBACK_REASON;
        }

        String normalizedReason = reason
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();

        return normalizedReason.isBlank() ? FALLBACK_REASON : normalizedReason;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record PlaceReasonContext(
            LocalDate visitDate,
            String departureAreaName,
            String companionType,
            List<String> preferenceCategories,
            String placeName,
            String placeType,
            String categoryName
    ) {
        public PlaceReasonContext {
            preferenceCategories = preferenceCategories == null ? List.of() : List.copyOf(preferenceCategories);
        }
    }

    public record CourseReasonContext(
            LocalDate visitDate,
            String departureAreaName,
            String companionType,
            List<String> preferenceCategories,
            List<String> placeNames
    ) {
        public CourseReasonContext {
            preferenceCategories = preferenceCategories == null ? List.of() : List.copyOf(preferenceCategories);
            placeNames = placeNames == null ? List.of() : List.copyOf(placeNames);
        }
    }

    public record FinalCourseReasonContext(
            LocalDate visitDate,
            String departureAreaName,
            String companionType,
            List<String> preferenceCategories,
            List<String> orderedPlaceNames
    ) {
        public FinalCourseReasonContext {
            preferenceCategories = preferenceCategories == null ? List.of() : List.copyOf(preferenceCategories);
            orderedPlaceNames = orderedPlaceNames == null ? List.of() : List.copyOf(orderedPlaceNames);
        }
    }

    public record FinalPlaceReasonContext(
            int sequence,
            String placeName,
            String placeType,
            String previousPlaceName,
            String nextPlaceName
    ) {
    }
}
