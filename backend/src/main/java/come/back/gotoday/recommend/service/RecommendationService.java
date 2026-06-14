package come.back.gotoday.recommend.service;

import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.preference.repository.UserPreferenceCategoryRepository;
import come.back.gotoday.preference.repository.UserPreferenceRepository;
import come.back.gotoday.recommend.engine.SearchUtils;
import come.back.gotoday.recommend.engine.VectorEmbeddingEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationService {

    // 의존성 주입을 위한 가상의 레포지토리 인터페이스 정의 (구현체에 맞추어 주입하세요)
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserPreferenceCategoryRepository userPreferenceCategoryRepository;
    private final EventRepository eventRepository;

    private final SearchUtils searchUtils;
    private final VectorEmbeddingEngine vectorEngine;

    public RecommendationService(UserPreferenceRepository userPreferenceRepository,
                                 UserPreferenceCategoryRepository userPreferenceCategoryRepository,
                                 EventRepository eventRepository,
                                 SearchUtils searchUtils,
                                 VectorEmbeddingEngine vectorEngine) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userPreferenceCategoryRepository = userPreferenceCategoryRepository;
        this.eventRepository = eventRepository;
        this.searchUtils = searchUtils;
        this.vectorEngine = vectorEngine;
    }

    @Transactional(readOnly = true)
    public List<Long> getRecommendedEventIds(Long memberId, String queryText,LocalDate searchStart, LocalDate searchEnd, int topK){
        // 1. 유저 선호 데이터 기반 쿼리 텍스트 빌드 (예시 1 방식)
            var preferenceOpt = userPreferenceRepository.findByMemberId(memberId);
        if (preferenceOpt.isEmpty()) return Collections.emptyList(); // 선호 정보가 없으면 빈 리스트 반환
        var preference = preferenceOpt.get();

        List<String> preferredCategories = userPreferenceCategoryRepository.findCategoryNamesByPreferenceId(preference.getId());
        String categoryJoined = String.join(", ", preferredCategories);

        // 추천 시스템의 정확도를 위해 하드 필터링(Hard Filtering) 수행 => 정해진 지역에서만 행사 추출
        String targetArea = preference.getPreferredArea();

        // 1단계: 선호 지역 + 기간 + 선호 카테고리 (가장 정확한 결과)
        List<Event> candidateEvents = eventRepository.findRecommendedEventsWithCategory(
                targetArea, searchStart, searchEnd, preferredCategories);

        // 2단계: 선호 카테고리에 해당하는 행사가 없다면? -> 카테고리 제약만 해제
        if (candidateEvents.isEmpty()) {
            log.info("2단계 결과 없음: 카테고리 제약 해제하여 재검색+기한은 유지");
            candidateEvents = eventRepository.findRecommendedEvents(targetArea, searchStart, searchEnd);
        }

        // 3단계: 선호 지역 데이터가 없으면, 전후 7일로 기간을 넓혀서 검색
        if (candidateEvents.isEmpty()) {
            log.info("3단계 결과 없음: 기간을 전후 7일로 확장하여 재검색");
            candidateEvents = eventRepository.findRecommendedEvents(targetArea, searchStart.minusDays(7), searchEnd.plusDays(7));
        }

        // 4단계: 여전히 없다면, 전체 지역의 해당 기간 데이터라도 검색 (서울 전체)
        if (candidateEvents.isEmpty()) {
            log.info("4단계 결과 없음: 전체 지역으로 검색 범위 확장");
            candidateEvents = eventRepository.findAllEventsByDate(searchStart, searchEnd);
        }

//        candidateEvents = eventRepository.findAll();

        log.info("검색 지역: {}, 기간: {} ~ {}", preference.getPreferredArea(), searchStart, searchEnd);
        log.info("필터링된 결과 개수: {}", candidateEvents.size());

        if (candidateEvents.isEmpty()) {
            log.warn("🚨 최종적으로 추천할 데이터가 없습니다.");
            return Collections.emptyList(); // todo 비지니스 예외처리를 해서 여기서 프론트에게 전달해야 한다. "원하시는 조합으로 서울 지역에서의 행사를 찾을 수 없습니다. 행사를 찾고 싶으시면 조정해서 다시 찾아주세요."
        }

        //쿼리 문장(사용자 선호에 대한 정보)을 핵심 단어로 쪼갠다.
        List<String> queryTokens = searchUtils.tokenize(queryText);

        //여기서는 쿼리 문장을 벡터화 시켜준다.
        float[] queryEmbedding = vectorEngine.getEmbedding(queryText);

        Map<Long, List<String>> docTokensMap = new HashMap<>();
        Map<Long, float[]> docEmbeddingMap = new HashMap<>();

        for (var event : candidateEvents) {
            String docText = String.format(
                    "[지역: %s] [카테고리: %s] [타겟/대상: %s] [행사명: %s]",
                    event.getArea(),
                    event.getCategory().getName(), // (필요 시 Event 엔티티에 추가)
                    event.getTarget(),
                    event.getTitle()
            );
            docTokensMap.put(event.getId(), searchUtils.tokenize(docText));

            // 배치에서 저장된 벡터값을 꺼내서 사용한다.
            docEmbeddingMap.put(event.getId(), event.getEmbeddingVector());
        }

        // 3. BM25 점수 및 Dense 유사도 산출
        Map<Long, Double> bm25Scores = searchUtils.calculateBM25(queryTokens, docTokensMap);
        Map<Long, Double> denseScores = new HashMap<>();
        for (var entry : docEmbeddingMap.entrySet()) {
            double sim = searchUtils.cosineSimilarity(queryEmbedding, entry.getValue());
            denseScores.put(entry.getKey(), sim);
        }

        // 4. 각 스코어별 순위 랭킹 리스트 정렬 생성
        List<Long> bm25Ranked = bm25Scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Long> denseRanked = denseScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 5. RRF(Reciprocal Rank Fusion) 스코어 계산
        Map<Long, Double> rrfScores = new HashMap<>();
        int k = 60; // 표준 상수 설정

//        // BM25 순위 점수 누적
//        for (int rank = 0; rank < bm25Ranked.size(); rank++) {
//            Long docId = bm25Ranked.get(rank);
//            rrfScores.put(docId, rrfScores.getOrDefault(docId, 0.0) + (1.0 / (k + (rank + 1))));
//        }
//
//        // Dense 임베딩 순위 점수 누적
//        for (int rank = 0; rank < denseRanked.size(); rank++) {
//            Long docId = denseRanked.get(rank);
//            rrfScores.put(docId, rrfScores.getOrDefault(docId, 0.0) + (1.0 / (k + (rank + 1))));
//        }

        // 가중치 설정 (두 값의 합은 1.0으로 맞추는 것이 좋습니다)
        double denseWeight = 0.7; // 의미 기반(AI)에 70% 비중
        double bm25Weight = 0.3;  // 키워드 기반에는 30% 비중
        // 카테고리 일치 여부에 따른 보너스 점수(Boost) 설정
        double categoryBoost = 2.0;

        // BM25 점수 계산 시 가중치 반영
        for (int rank = 0; rank < bm25Ranked.size(); rank++) {
            Long docId = bm25Ranked.get(rank);
            double score = 1.0 / (k + (rank + 1));
            rrfScores.put(docId, rrfScores.getOrDefault(docId, 0.0) + (score * bm25Weight));
        }

        // Dense 임베딩 점수 계산 시 가중치 반영
        for (int rank = 0; rank < denseRanked.size(); rank++) {
            Long docId = denseRanked.get(rank);
            double score = 1.0 / (k + (rank + 1));
            rrfScores.put(docId, rrfScores.getOrDefault(docId, 0.0) + (score * denseWeight));
        }

        // 카테고리 하드 필터링
        for (var event : candidateEvents) {
            if (rrfScores.containsKey(event.getId()) && preferredCategories.contains(event.getCategory().getName())) {
                double currentScore = rrfScores.get(event.getId());
                rrfScores.put(event.getId(), currentScore * categoryBoost);
            }
        }
        // 6. RRF 점수가 높은 최종 융합 상위 N개 아이템 ID 정렬 후 반환
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public String createQueryText(String baseArea, String categories, String companionType) {
        // 기본값 방어 전략
        String area = (baseArea != null && !baseArea.isBlank()) ? baseArea : "서울";
        String cats = (categories != null && !categories.isBlank()) ? categories : "전체";
        String companion = (companionType != null && !companionType.isBlank()) ? companionType : "누구나";

        // 데이터 적재(Batch) 시점의 포맷과 쌍을 이루는 템플릿 구조 (서술어 없는 메타 태그 형태)
        // 예: "[지역: 성수동] [카테고리: 산책, 공연] [타겟/대상: 커플]"
        return String.format(
                "[지역: %s] [카테고리: %s] [타겟/대상: %s]",
                area, cats, companion
        );
    }


}