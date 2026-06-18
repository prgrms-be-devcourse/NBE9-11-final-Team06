package come.back.gotoday.recommend.service;

import come.back.gotoday.category.repository.PreferenceEventCategoryMappingRepository;

import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.entity.CoursePlace;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.crowd.service.CrowdScoreCalculator;
import come.back.gotoday.crowd.service.NearestCrowdAreaService;
import come.back.gotoday.crowd.util.GeoDistanceCalculator;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.event.service.EventScheduleMatcher;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationService {

    private static final double AVOID_CROWD_PREFERENCE_WEIGHT = 0.5;
    private static final double AVOID_CROWD_WEIGHT = 0.3;
    private static final double AVOID_CROWD_DISTANCE_WEIGHT = 0.2;
    private static final double INDIFFERENT_PREFERENCE_WEIGHT = 0.7;
    private static final double INDIFFERENT_DISTANCE_WEIGHT = 0.3;
    private static final double DEFAULT_CROWD_SCORE = 0.5;

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserPreferenceCategoryRepository userPreferenceCategoryRepository;
    private final PreferenceEventCategoryMappingRepository preferenceEventCategoryMappingRepository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;
    private final PlaceRepository placeRepository;
    private final NearestCrowdAreaService nearestCrowdAreaService;
    private final CrowdScoreCalculator crowdScoreCalculator;

    private final EventScheduleMatcher eventScheduleMatcher;
    private final SearchUtils searchUtils;
    private final VectorEmbeddingEngine vectorEngine;

    public RecommendationService(UserPreferenceRepository userPreferenceRepository,
                                 UserPreferenceCategoryRepository userPreferenceCategoryRepository,
                                 PreferenceEventCategoryMappingRepository preferenceEventCategoryMappingRepository,
                                 EventRepository eventRepository,
                                 MemberRepository memberRepository,
                                 CourseRepository courseRepository,
                                 PlaceRepository placeRepository,
                                 NearestCrowdAreaService nearestCrowdAreaService,
                                 CrowdScoreCalculator crowdScoreCalculator,
                                 EventScheduleMatcher eventScheduleMatcher,
                                 SearchUtils searchUtils,
                                 VectorEmbeddingEngine vectorEngine) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userPreferenceCategoryRepository = userPreferenceCategoryRepository;
        this.preferenceEventCategoryMappingRepository = preferenceEventCategoryMappingRepository;
        this.eventRepository = eventRepository;
        this.memberRepository = memberRepository;
        this.courseRepository = courseRepository;
        this.placeRepository = placeRepository;
        this.nearestCrowdAreaService = nearestCrowdAreaService;
        this.crowdScoreCalculator = crowdScoreCalculator;
        this.eventScheduleMatcher = eventScheduleMatcher;
        this.searchUtils = searchUtils;
        this.vectorEngine = vectorEngine;
    }

    @Transactional
    public RecommendationCourseResponse createRecommendedCourse(Long memberId, RecommendationCourseCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        var preferenceOptional = userPreferenceRepository.findByMemberId(memberId);

        List<String> savedCategories = preferenceOptional
                .map(preference -> userPreferenceCategoryRepository.findCategoryNamesByPreferenceId(preference.getId()))
                .orElseGet(Collections::emptyList);

        String selectedArea = hasText(request.area())
                ? request.area()
                : preferenceOptional.map(preference -> preference.getPreferredArea()).orElse(null);

        List<String> selectedCategories = request.categories() != null && !request.categories().isEmpty()
                ? request.categories()
                : savedCategories;

        Set<Long> preferredEventCategoryIds = selectedCategories.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(preferenceEventCategoryMappingRepository
                        .findEventCategoryIdsByPreferenceCategoryNames(selectedCategories));

        String selectedCompanionType = hasText(request.companionType())
                ? request.companionType()
                : preferenceOptional
                        .map(preference -> preference.getCompanionType() != null
                                ? preference.getCompanionType().toString()
                                : null)
                        .orElse(null);

        boolean avoidCrowds = preferenceOptional
                .map(preference -> Boolean.TRUE.equals(preference.getAvoidCrowded()))
                .orElse(false);

        String queryText = createQueryText(
                selectedArea,
                String.join(", ", selectedCategories),
                selectedCompanionType
        );

        List<Long> recommendedEventIds = getRecommendedEventIds(
                selectedArea,
                preferredEventCategoryIds,
                queryText,
                request.startDate(),
                request.endDate(),
                request.latitude(),
                request.longitude(),
                avoidCrowds,
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
                selectedArea,
                selectedCompanionType,
                null,
                null,
                "현재 선택한 조건과 행사 유사도를 기반으로 추천되었습니다."
        );

        List<RecommendedCoursePlaceResponse> places = new ArrayList<>();
        int visitOrder = 1;

        for (Event event : recommendedEvents) {
            Place place = getOrCreatePlaceFromEvent(event);
            String reason = createRecommendationReason(
                    event,
                    selectedArea,
                    preferredEventCategoryIds
            );

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

    private String createRecommendationReason(
            Event event,
            String preferredArea,
            Set<Long> preferredEventCategoryIds
    ) {
        boolean areaMatched = preferredArea != null && preferredArea.equals(event.getArea());
        boolean categoryMatched = event.getCategory() != null
                && preferredEventCategoryIds.contains(event.getCategory().getId());

        if (areaMatched && categoryMatched) {
            return "선택한 지역과 카테고리에 모두 부합하는 행사입니다.";
        }

        if (areaMatched) {
            return "선택한 지역에 맞는 행사입니다.";
        }

        if (categoryMatched) {
            return "선택한 카테고리에 맞는 행사입니다.";
        }

        return "현재 선택한 조건과 행사 유사도를 기반으로 추천되었습니다.";
    }

    @Transactional(readOnly = true)
    public List<Long> getRecommendedEventIds(Long memberId, String queryText, LocalDate searchStart, LocalDate searchEnd, int topK) {
        var preference = userPreferenceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        List<Long> preferenceCategoryIds = userPreferenceCategoryRepository
                .findCategoryIdsByPreferenceId(preference.getId());
        Set<Long> preferredEventCategoryIds = preferenceCategoryIds.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(preferenceEventCategoryMappingRepository
                        .findEventCategoryIdsByPreferenceCategoryIds(preferenceCategoryIds));
        boolean avoidCrowds = Boolean.TRUE.equals(preference.getAvoidCrowded());
        return getRecommendedEventIds(
                preference.getPreferredArea(),
                preferredEventCategoryIds,
                queryText,
                searchStart,
                searchEnd,
                null,
                null,
                avoidCrowds,
                topK
        );
    }

    private List<Long> getRecommendedEventIds(
            String targetArea,
            Set<Long> preferredEventCategoryIds,
            String queryText,
            LocalDate searchStart,
            LocalDate searchEnd,
            Double startLatitude,
            Double startLongitude,
            boolean avoidCrowds,
            int topK
    ) {
        List<Event> candidateEvents = preferredEventCategoryIds.isEmpty()
                ? Collections.emptyList()
                : eventRepository.findRecommendedEventsWithCategoryIds(
                        targetArea,
                        searchStart,
                        searchEnd,
                        preferredEventCategoryIds
                );

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

        log.info("검색 지역: {}, 기간: {} ~ {}", targetArea, searchStart, searchEnd);
        log.info("필터링된 결과 개수: {}", candidateEvents.size());

        if (candidateEvents.isEmpty()) {
            log.warn("최종적으로 추천할 데이터가 없습니다.");
            return Collections.emptyList();
        }
        Set<DayOfWeek> userCourseDays = eventScheduleMatcher.getDaysOfWeekInPeriod(searchStart, searchEnd);

        List<Event> dayOfWeekFilteredEvents = candidateEvents.stream()
                .filter(event -> eventScheduleMatcher.isEventAvailableOnDays(
                        event.getEventTime(),
                        searchStart,
                        searchEnd,
                        userCourseDays
                ))
                .collect(Collectors.toList());

        // 만약 요일 필터링을 거쳤더니 남은 행사가 0건인 경우 (선택 B: 원래 데이터를 복구하고 알림 플래그를 false로 전환)
        if (dayOfWeekFilteredEvents.isEmpty()) {
            log.info("요일 하드 필터링 결과 매칭되는 행사가 0건입니다. 선택 B 정책에 따라 요일 필터를 해제하고 차선책을 제공합니다.");
            // candidateEvents 원본을 그대로 유지하여 형태 보존 처리
        } else {
            log.info("요일 하드 필터링 통과 완료. 남은 후보 개수: {}/{}", dayOfWeekFilteredEvents.size(), candidateEvents.size());
            candidateEvents = dayOfWeekFilteredEvents; // 필터링된 컬렉션으로 교체
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
            if (rrfScores.containsKey(event.getId())
                    && event.getCategory() != null
                    && preferredEventCategoryIds.contains(event.getCategory().getId())) {
                double currentScore = rrfScores.get(event.getId());
                rrfScores.put(event.getId(), currentScore * categoryBoost);
            }
        }

        return selectGreedyEventIds(
                candidateEvents,
                rrfScores,
                startLatitude,
                startLongitude,
                avoidCrowds,
                topK
        );
    }

    /**
     * 기존 검색 관련도 점수를 선호 점수로 유지하면서 현재 위치와의 거리까지
     * 매 선택 단계마다 다시 계산해 다음 행사를 고르는 그리디 선택 로직입니다.
     *
     * 시작 좌표가 없으면 첫 번째 행사는 선호 점수가 가장 높은 후보를 선택하고,
     * 이후부터는 직전에 선택된 행사의 좌표를 현재 위치로 사용합니다.
     */
    private List<Long> selectGreedyEventIds(
            List<Event> candidateEvents,
            Map<Long, Double> preferenceScores,
            Double startLatitude,
            Double startLongitude,
            boolean avoidCrowds,
            int topK
    ) {
        if (topK <= 0 || candidateEvents.isEmpty()) {
            return Collections.emptyList();
        }

        double maxPreferenceScore = preferenceScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        List<GreedyCandidate> candidates = candidateEvents.stream()
                .filter(event -> preferenceScores.containsKey(event.getId()))
                .map(event -> new GreedyCandidate(
                        event,
                        normalizePreferenceScore(
                                preferenceScores.getOrDefault(event.getId(), 0.0),
                                maxPreferenceScore
                        ),
                        avoidCrowds ? calculateCrowdScore(event) : 0.0
                ))
                .toList();

        List<Long> selectedEventIds = new ArrayList<>();
        Set<Long> selectedIds = new HashSet<>();
        Double currentLatitude = startLatitude;
        Double currentLongitude = startLongitude;

        while (selectedEventIds.size() < topK && selectedIds.size() < candidates.size()) {
            Double scoringLatitude = currentLatitude;
            Double scoringLongitude = currentLongitude;

            GreedyCandidate selected = candidates.stream()
                    .filter(candidate -> !selectedIds.contains(candidate.event().getId()))
                    .max(Comparator
                            .comparingDouble((GreedyCandidate candidate) -> calculateGreedyScore(
                                    candidate,
                                    scoringLatitude,
                                    scoringLongitude,
                                    avoidCrowds
                            ))
                            .thenComparingLong(candidate -> -candidate.event().getId()))
                    .orElse(null);

            if (selected == null) {
                break;
            }

            Event selectedEvent = selected.event();
            selectedEventIds.add(selectedEvent.getId());
            selectedIds.add(selectedEvent.getId());

            if (hasCoordinate(selectedEvent)) {
                currentLatitude = selectedEvent.getLatitude();
                currentLongitude = selectedEvent.getLongitude();
            }
        }

        return selectedEventIds;
    }

    private double calculateGreedyScore(
            GreedyCandidate candidate,
            Double currentLatitude,
            Double currentLongitude,
            boolean avoidCrowds
    ) {
        double distanceScore = calculateDistanceScore(
                candidate.event(),
                currentLatitude,
                currentLongitude
        );

        if (avoidCrowds) {
            return (candidate.preferenceScore() * AVOID_CROWD_PREFERENCE_WEIGHT)
                    + (candidate.crowdScore() * AVOID_CROWD_WEIGHT)
                    + (distanceScore * AVOID_CROWD_DISTANCE_WEIGHT);
        }

        return (candidate.preferenceScore() * INDIFFERENT_PREFERENCE_WEIGHT)
                + (distanceScore * INDIFFERENT_DISTANCE_WEIGHT);
    }

    private double calculateCrowdScore(Event event) {
        if (!hasCoordinate(event)) {
            return DEFAULT_CROWD_SCORE;
        }

        return nearestCrowdAreaService.findNearest(
                        event.getLatitude(),
                        event.getLongitude()
                )
                .map(nearest -> crowdScoreCalculator.calculate(nearest.congestionLevel()) / 100.0)
                .orElse(DEFAULT_CROWD_SCORE);
    }

    private double calculateDistanceScore(
            Event event,
            Double currentLatitude,
            Double currentLongitude
    ) {
        if (currentLatitude == null || currentLongitude == null || !hasCoordinate(event)) {
            return 0.0;
        }

        double distanceKm = GeoDistanceCalculator.calculateKilometers(
                currentLatitude,
                currentLongitude,
                event.getLatitude(),
                event.getLongitude()
        );

        return 1.0 / (1.0 + distanceKm);
    }

    private double normalizePreferenceScore(double score, double maxScore) {
        if (maxScore <= 0.0) {
            return 0.0;
        }
        return score / maxScore;
    }

    private boolean hasCoordinate(Event event) {
        return event.getLatitude() != null && event.getLongitude() != null;
    }

    private record GreedyCandidate(
            Event event,
            double preferenceScore,
            double crowdScore
    ) {
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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