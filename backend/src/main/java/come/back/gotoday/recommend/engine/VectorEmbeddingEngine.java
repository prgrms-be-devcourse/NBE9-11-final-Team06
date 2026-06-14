package come.back.gotoday.recommend.engine;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class VectorEmbeddingEngine {

    private final RestClient restClient;
    // 허깅페이스 스페이스 다이렉트 URL 주소 설정
    private static final String HUGGING_FACE_URL = "https://ccodeer-vectorai.hf.space";

    public VectorEmbeddingEngine() {
        //배치할때는 타임아웃이 길게 잡혀야 하고 사용자 요청의 경우는 짧게 잡혀야한다.
        this.restClient = RestClient.builder()
                .baseUrl(HUGGING_FACE_URL)
                .build();
    }

    /**
     * [단일 텍스트 변환] 추천 서비스(RecommendationService)에서 유저 쿼리를 변환할 때 사용합니다.
     */
    public float[] getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[0];
        }
        List<float[]> results = getEmbeddings(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    /**
     * [대량 텍스트 변환 (배치용)] 배치 프로세스에서 1000개의 청크 문장들을 한 번에 변환할 때 사용합니다.
     */
    public List<float[]> getEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            // 허깅페이스 FastAPI 규격 {"texts": [...]} 에 맞추어 요청 객체 생성
            Map<String, List<String>> requestBody = Map.of("texts", texts);

            // RestClient를 이용한 동기(Blocking) 호출 진행
            Map<String, List<List<Double>>> response = restClient.post()
                    .uri("/embedding")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, List<List<Double>>>>() {});

            if (response == null || !response.containsKey("embeddings")) {
                throw new RuntimeException("허깅페이스 응답 형식이 올바르지 않습니다.");
            }

            // List<List<Double>> 형태의 응답을 기존 자바 시스템 호환 규격인 List<float[]>로 파싱
            List<List<Double>> rawEmbeddings = response.get("embeddings");
            return rawEmbeddings.stream()
                    .map(list -> {
                        float[] vector = new float[list.size()];
                        for (int i = 0; i < list.size(); i++) {
                            vector[i] = list.get(i).floatValue();
                        }
                        return vector;
                    })
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("허깅페이스 API 통신 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}