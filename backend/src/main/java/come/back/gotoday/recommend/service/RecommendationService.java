package come.back.gotoday.recommend.service;

import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.entity.CoursePlace;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import come.back.gotoday.preference.repository.UserPreferenceCategoryRepository;
import come.back.gotoday.preference.repository.UserPreferenceRepository;
import come.back.gotoday.recommend.dto.RecommendationCourseCreateRequest;
import come.back.gotoday.recommend.dto.RecommendationCourseResponse;
import come.back.gotoday.recommend.dto.RecommendedCoursePlaceResponse;
import come.back.gotoday.recommend.engine.SearchUtils;
import come.back.gotoday.recommend.engine.VectorEmbeddingEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserPreferenceCategoryRepository userPreferenceCategoryRepository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;
    private final PlaceRepository placeRepository;

    private final SearchUtils searchUtils;
    private final VectorEmbeddingEngine vectorEngine;

    public RecommendationService(UserPreferenceRepository userPreferenceRepository,
                                 UserPreferenceCategoryRepository userPreferenceCategoryRepository,
                                 EventRepository eventRepository,
                                 MemberRepository memberRepository,
                                 CourseRepository courseRepository,
                                 PlaceRepository placeRepository,
                                 SearchUtils searchUtils,
                                 VectorEmbeddingEngine vectorEngine) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userPreferenceCategoryRepository = userPreferenceCategoryRepository;
        this.eventRepository = eventRepository;
        this.memberRepository = memberRepository;
        this.courseRepository = courseRepository;
        this.placeRepository = placeRepository;
        this.searchUtils = searchUtils;
        this.vectorEngine = vectorEngine;
    }

    @Transactional
    public RecommendationCourseResponse createRecommendedCourse(Long memberId, RecommendationCourseCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        var preference = userPreferenceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        List<String> preferredCategories = userPreferenceCategoryRepository.findCategoryNamesByPreferenceId(preference.getId());
        String categoryJoined = String.join(", ", preferredCategories);
        String companionType = preference.getCompanionType() != null ? preference.getCompanionType().toString() : null;
        String queryText = createQueryText(
                preference.getPreferredArea(),
                categoryJoined,
                companionType
        );

        List<Long> recommendedEventIds = getRecommendedEventIds(
                memberId,
                queryText,
                request.startDate(),
                request.endDate(),
                request.getTopKOrDefault()
        );

        if (recommendedEventIds.isEmpty()) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_EVENT_NOT_FOUND);
        }

        Map<Long, Event> eventMap = eventRepository.findAllById(recommendedEventIds).stream()
                .collect(Collectors.toMap(Event::getId, event -> event));

        List<Event> recommendedEvents = recommendedEventIds.stream()
                .map(eventMap::get)
                .filter(Objects::nonNull)
                .toList();

        Course course = Course.create(
                member,
                request.getTitleOrDefault(),
                "추천 알고리즘으로 생성된 코스입니다.",
                "RECOMMENDATION",
                request.startDate(),
                request.endDate(),
                preference.getPreferredArea(),
                companionType,
                null,
                null,
                "사용자 선호 정보와 행사 유사도를 기반으로 추천되었습니다."
        );

        List<RecommendedCoursePlaceResponse> places = new ArrayList<>();
        int visitOrder = 1;

        for (Event event : recommendedEvents) {
            Place place = getOrCreatePlaceFromEvent(event);
            String reason = createRecommendationReason(event, preference.getPreferredArea(), preferredCategories);

            CoursePlace coursePlace = CoursePlace.create(
                    course,
                    place,
                    event,
                    visitOrder,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    reason
            );
            course.addCoursePlace(coursePlace);

            places.add(new RecommendedCoursePlaceResponse(
                    event.getId(),
                    place.getId(),
                    event.getTitle(),
                    event.getCategory().getName(),
                    event.getArea(),
                    event.getStartDate(),
                    event.getEndDate(),
                    place.getLatitude(),
                    place.getLongitude(),
                    visitOrder,
                    reason
            ));

            visitOrder++;
        }

        Course savedCourse = courseRepository.save(course);

        return new RecommendationCourseResponse(
                savedCourse.getId(),
                savedCourse.getTitle(),
                savedCourse.getStartDate(),
                savedCourse.getEndDate(),
                places
        );
    }

    private Place getOrCreatePlaceFromEvent(Event event) {
        if (event.getPlace() != null) {
            return event.getPlace();
        }

        Place place = Place.create(
                event.getCategory(),
                event.getTitle(),
                event.getArea(),
                null,
                toBigDecimal(event.getLatitude()),
                toBigDecimal(event.getLongitude()),
                null,
                event.getHomepageUrl(),
                event.getDescription(),
                String.valueOf(event.getSource()),
                event.getExternalId(),
                true
        );

        Place savedPlace = placeRepository.save(place);
        event.updatePlace(savedPlace);
        return savedPlace;
    }

    private BigDecimal toBigDecimal(Double coordinate) {
        if (coordinate == null) {
            return null;
        }
        return BigDecimal.valueOf(coordinate);
    }

    private String createRecommendationReason(Event event, String preferredArea, List<String> preferredCategories) {
        boolean areaMatched = preferredArea != null && preferredArea.equals(event.getArea());
        boolean categoryMatched = event.getCategory() != null
                && preferredCategories.contains(event.getCategory().getName());

        if (areaMatched && categoryMatched) {
            return "선호 지역과 선호 카테고리에 모두 부합하는 행사입니다.";
        }

        if (areaMatched) {
            return "선호 지역에 맞는 행사입니다.";
        }

        if (categoryMatched) {
            return "선호 카테고리에 맞는 행사입니다.";
        }

        return "사용자 선호 정보와 행사 유사도를 기반으로 추천되었습니다.";
    }

    @Transactional(readOnly = true)
    public List<Long> getRecommendedEventIds(Long memberId, String queryText, LocalDate searchStart, LocalDate searchEnd, int topK) {
        var preference = userPreferenceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        List<String> preferredCategories = userPreferenceCategoryRepository.findCategoryNamesByPreferenceId(preference.getId());
        String targetArea = preference.getPreferredArea();

        List<Event> candidateEvents = eventRepository.findRecommendedEventsWithCategory(
                targetArea, searchStart, searchEnd, preferredCategories);

        if (candidateEvents.isEmpty()) {
            log.info("2단계 결과 없음: 카테고리 제약 해제하여 재검색+기한은 유지");
            candidateEvents = eventRepository.findRecommendedEvents(targetArea, searchStart, searchEnd);
        }

        if (candidateEvents.isEmpty()) {
            log.info("3단계 결과 없음: 기간을 전후 7일로 확장하여 재검색");
            candidateEvents = eventRepository.findRecommendedEvents(targetArea, searchStart.minusDays(7), searchEnd.plusDays(7));
        }

        if (candidateEvents.isEmpty()) {
            log.info("4단계 결과 없음: 전체 지역으로 검색 범위 확장");
            candidateEvents = eventRepository.findAllEventsByDate(searchStart, searchEnd);
        }

        log.info("검색 지역: {}, 기간: {} ~ {}", preference.getPreferredArea(), searchStart, searchEnd);
        log.info("필터링된 결과 개수: {}", candidateEvents.size());

        if (candidateEvents.isEmpty()) {
            log.warn("최종적으로 추천할 데이터가 없습니다.");
            return Collections.emptyList();
        }

        List<String> queryTokens = searchUtils.tokenize(queryText);
        float[] queryEmbedding = vectorEngine.getEmbedding(queryText);

        Map<Long, List<String>> docTokensMap = new HashMap<>();
        Map<Long, float[]> docEmbeddingMap = new HashMap<>();

        for (var event : candidateEvents) {
            String docText = String.format(
                    "[지역: %s] [카테고리: %s] [타겟/대상: %s] [행사명: %s]",
                    event.getArea(),
                    event.getCategory().getName(),
                    event.getTarget(),
                    event.getTitle()
            );
            docTokensMap.put(event.getId(), searchUtils.tokenize(docText));

            if (event.getEmbeddingVector() != null) {
                docEmbeddingMap.put(event.getId(), event.getEmbeddingVector());
            }
        }

        Map<Long, Double> bm25Scores = searchUtils.calculateBM25(queryTokens, docTokensMap);
        Map<Long, Double> denseScores = new HashMap<>();
        for (var entry : docEmbeddingMap.entrySet()) {
            double sim = searchUtils.cosineSimilarity(queryEmbedding, entry.getValue());
            denseScores.put(entry.getKey(), sim);
        }

        List<Long> bm25Ranked = bm25Scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Long> denseRanked = denseScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<Long, Double> rrfScores = new HashMap<>();
        int k = 60;
        double denseWeight = 0.7;
        double bm25Weight = 0.3;
        double categoryBoost = 2.0;

        for (int rank = 0; rank < bm25Ranked.size(); rank++) {
            Long docId = bm25Ranked.get(rank);
            double score = 1.0 / (k + (rank + 1));
            rrfScores.put(docId, rrfScores.getOrDefault(docId, 0.0) + (score * bm25Weight));
        }

        for (int rank = 0; rank < denseRanked.size(); rank++) {
            Long docId = denseRanked.get(rank);
            double score = 1.0 / (k + (rank + 1));
            rrfScores.put(docId, rrfScores.getOrDefault(docId, 0.0) + (score * denseWeight));
        }

        for (var event : candidateEvents) {
            if (rrfScores.containsKey(event.getId()) && preferredCategories.contains(event.getCategory().getName())) {
                double currentScore = rrfScores.get(event.getId());
                rrfScores.put(event.getId(), currentScore * categoryBoost);
            }
        }

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public String createQueryText(String baseArea, String categories, String companionType) {
        String area = (baseArea != null && !baseArea.isBlank()) ? baseArea : "서울";
        String cats = (categories != null && !categories.isBlank()) ? categories : "전체";
        String companion = (companionType != null && !companionType.isBlank()) ? companionType : "누구나";

        return String.format(
                "[지역: %s] [카테고리: %s] [타겟/대상: %s]",
                area, cats, companion
        );
    }
}