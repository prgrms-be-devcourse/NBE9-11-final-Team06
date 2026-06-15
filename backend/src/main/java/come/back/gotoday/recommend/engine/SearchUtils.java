package come.back.gotoday.recommend.engine;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SearchUtils {

    private final Komoran komoran = new Komoran(DEFAULT_MODEL.LIGHT);

    // 1. Komoran을 이용한 한국어 명사/동사/형용사 토큰화
    public List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) return Collections.emptyList();
        try {
            KomoranResult result = komoran.analyze(text);
            // 추천에 유의미한 명사(NNG, NNP) 추출
            return result.getNouns();
        } catch (Exception e) {
            return Arrays.asList(text.split("\\s+")); // 예외 시 공백 분할 대치
        }
    }

    // 2. BM25 점수 계산 알고리즘
    public Map<Long, Double> calculateBM25(List<String> queryTokens, Map<Long, List<String>> documentTokensMap) {
        Map<Long, Double> scores = new HashMap<>();
        int docCount = documentTokensMap.size();
        if (docCount == 0) return scores;

        // 문서 길이 및 평균 길이 계산
        Map<Long, Integer> docLengths = new HashMap<>();
        double avgDocLength = 0.0;
        Map<String, Integer> docFreqs = new HashMap<>(); // 각 단어의 문서 빈도(DF)

        for (Map.Entry<Long, List<String>> entry : documentTokensMap.entrySet()) {
            List<String> tokens = entry.getValue();
            docLengths.put(entry.getKey(), tokens.size());
            avgDocLength += tokens.size();

            // 중복 제거하여 DF 카운트
            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String token : uniqueTokens) {
                docFreqs.put(token, docFreqs.getOrDefault(token, 0) + 1);
            }
        }
        avgDocLength /= docCount;

        // BM25 하이퍼파라미터
        double k1 = 1.2;
        double b = 0.75;//짧은 문서는 낮게 주어야 한다. 0.75 default

        // 쿼리 토큰 기준으로 점수 산출
        for (String token : queryTokens) {
            int df = docFreqs.getOrDefault(token, 0);
            if (df == 0) continue;

            // IDF 계산 (소수점 음수 방지 처리)
            double idf = Math.log(1.0 + (docCount - df + 0.5) / (df + 0.5));

            for (Map.Entry<Long, List<String>> entry : documentTokensMap.entrySet()) {
                Long docId = entry.getKey();
                List<String> tokens = entry.getValue();

                // Term Frequency (TF)
                long tf = tokens.stream().filter(t -> t.equals(token)).count();
                if (tf == 0) continue;

                int docLen = docLengths.get(docId);
                double score = idf * (tf * (k1 + 1)) / (tf + k1 * (1 - b + b * (docLen / avgDocLength)));
                scores.put(docId, scores.getOrDefault(docId, 0.0) + score);
            }
        }
        return scores;
    }

    // 3. 코사인 유사도 계산 알고리즘
    public double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length || vectorA.length == 0) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }


}