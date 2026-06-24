package come.back.gotoday.recommend.service;

import come.back.gotoday.category.repository.PreferenceEventCategoryMappingRepository;

import come.back.gotoday.crowd.service.CrowdScoreCalculator;
import come.back.gotoday.crowd.service.NearestCrowdAreaService;
import come.back.gotoday.crowd.util.GeoDistanceCalculator;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.event.service.EventScheduleMatcher;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.preference.repository.UserPreferenceCategoryRepository;
import come.back.gotoday.preference.repository.UserPreferenceRepository;
import come.back.gotoday.recommend.dto.RecommendationCourseCreateRequest;
import come.back.gotoday.recommend.engine.SearchUtils;
import come.back.gotoday.recommend.engine.VectorEmbeddingEngine;
import come.back.gotoday.weather.model.WeatherCondition;
import come.back.gotoday.weather.service.EventIndoorOutdoorPolicy;
import come.back.gotoday.weather.service.WeatherConditionClassifier;
import come.back.gotoday.weather.service.WeatherForecastService;
import come.back.gotoday.weather.service.WeatherScoreCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationService {

    private static final double AVOID_CROWD_PREFERENCE_WEIGHT = 0.4;
    private static final double AVOID_CROWD_WEIGHT = 0.25;
    private static final double AVOID_CROWD_DISTANCE_WEIGHT = 0.2;
    private static final double AVOID_CROWD_WEATHER_WEIGHT = 0.15;
    private static final double INDIFFERENT_PREFERENCE_WEIGHT = 0.55;
    private static final double INDIFFERENT_DISTANCE_WEIGHT = 0.3;
    private static final double INDIFFERENT_WEATHER_WEIGHT = 0.15;
    private static final double DEFAULT_CROWD_SCORE = 0.5;
    private static final double DEFAULT_WEATHER_SCORE = 0.5;
    private static final int DEFAULT_BEAM_WIDTH = 5;

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserPreferenceCategoryRepository userPreferenceCategoryRepository;
    private final PreferenceEventCategoryMappingRepository preferenceEventCategoryMappingRepository;
    private final EventRepository eventRepository;
    private final NearestCrowdAreaService nearestCrowdAreaService;
    private final CrowdScoreCalculator crowdScoreCalculator;

    private final EventScheduleMatcher eventScheduleMatcher;
    private final SearchUtils searchUtils;
    private final VectorEmbeddingEngine vectorEngine;
    private final WeatherForecastService weatherForecastService;
    private final WeatherConditionClassifier weatherConditionClassifier;
    private final WeatherScoreCalculator weatherScoreCalculator;
    private final EventIndoorOutdoorPolicy eventIndoorOutdoorPolicy;

    public RecommendationService(UserPreferenceRepository userPreferenceRepository,
                                 UserPreferenceCategoryRepository userPreferenceCategoryRepository,
                                 PreferenceEventCategoryMappingRepository preferenceEventCategoryMappingRepository,
                                 EventRepository eventRepository,
                                 NearestCrowdAreaService nearestCrowdAreaService,
                                 CrowdScoreCalculator crowdScoreCalculator,
                                 EventScheduleMatcher eventScheduleMatcher,
                                 SearchUtils searchUtils,
                                 VectorEmbeddingEngine vectorEngine,
                                 WeatherForecastService weatherForecastService,
                                 WeatherConditionClassifier weatherConditionClassifier,
                                 WeatherScoreCalculator weatherScoreCalculator,
                                 EventIndoorOutdoorPolicy eventIndoorOutdoorPolicy) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userPreferenceCategoryRepository = userPreferenceCategoryRepository;
        this.preferenceEventCategoryMappingRepository = preferenceEventCategoryMappingRepository;
        this.eventRepository = eventRepository;
        this.nearestCrowdAreaService = nearestCrowdAreaService;
        this.crowdScoreCalculator = crowdScoreCalculator;
        this.eventScheduleMatcher = eventScheduleMatcher;
        this.searchUtils = searchUtils;
        this.vectorEngine = vectorEngine;
        this.weatherForecastService = weatherForecastService;
        this.weatherConditionClassifier = weatherConditionClassifier;
        this.weatherScoreCalculator = weatherScoreCalculator;
        this.eventIndoorOutdoorPolicy = eventIndoorOutdoorPolicy;
    }

    @Transactional(readOnly = true)
    public RecommendedCourseDraft recommendCourse(Long memberId, RecommendationCourseCreateRequest request) {
        var preferenceOptional = userPreferenceRepository.findByMemberId(memberId);

        List<String> savedCategories = preferenceOptional
                .map(preference -> userPreferenceCategoryRepository.findCategoryNamesByPreferenceId(preference.getId()))
                .orElseGet(Collections::emptyList);

        String rawSelectedArea = hasText(request.area())
                ? request.area()
                : preferenceOptional.map(preference -> preference.getPreferredArea()).orElse(null);
        String selectedArea = normalizeRecommendationArea(rawSelectedArea);

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

        Set<String> requestedEventCategoryNames = request.categories() == null
                ? Collections.emptySet()
                : request.categories().stream()
                  .filter(this::hasText)
                  .map(String::trim)
                  .collect(Collectors.toCollection(LinkedHashSet::new));

        log.info(
                "[행사 추천 요청 조건] requestArea={}, selectedArea={}, requestCategories={}, selectedCategories={}, requestedEventCategoryNames={}, mappedEventCategoryIds={}, companionType={}",
                request.area(),
                selectedArea,
                request.categories(),
                selectedCategories,
                requestedEventCategoryNames,
                preferredEventCategoryIds,
                selectedCompanionType
        );

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
                requestedEventCategoryNames,
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

        List<RecommendedEvent> recommendedEvents = new ArrayList<>();
        int visitOrder = 1;
        for (Long eventId : recommendedEventIds) {
            Event event = eventMap.get(eventId);
            if (event == null) {
                continue;
            }

            recommendedEvents.add(new RecommendedEvent(
                    event.getId(),
                    createRecommendationReason(
                            event,
                            selectedArea,
                            preferredEventCategoryIds,
                            requestedEventCategoryNames
                    ),
                    visitOrder++
            ));
        }

        if (recommendedEvents.isEmpty()) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_EVENT_NOT_FOUND);
        }

        return new RecommendedCourseDraft(
                request.getTitleOrDefault(),
                request.startDate(),
                request.endDate(),
                selectedArea,
                selectedCompanionType,
                request.latitude(),
                request.longitude(),
                List.copyOf(recommendedEvents)
        );
    }


    private String createRecommendationReason(
            Event event,
            String preferredArea,
            Set<Long> preferredEventCategoryIds,
            Set<String> requestedEventCategoryNames
    ) {
        boolean areaMatched = preferredArea != null && preferredArea.equals(event.getArea());
        boolean categoryMatched = matchesRequestedCategory(
                event,
                preferredEventCategoryIds,
                requestedEventCategoryNames
        );

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

    List<Long> getRecommendedEventIds(
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
        return getRecommendedEventIds(
                normalizeRecommendationArea(targetArea),
                preferredEventCategoryIds,
                Collections.emptySet(),
                queryText,
                searchStart,
                searchEnd,
                startLatitude,
                startLongitude,
                avoidCrowds,
                topK
        );
    }

    List<Long> getRecommendedEventIds(
            String targetArea,
            Set<Long> preferredEventCategoryIds,
            Set<String> requestedEventCategoryNames,
            String queryText,
            LocalDate searchStart,
            LocalDate searchEnd,
            Double startLatitude,
            Double startLongitude,
            boolean avoidCrowds,
            int topK
    ) {
        log.info(
                "[행사 추천 입력] targetArea={}, categoryIds={}, categoryNames={}, date={}~{}, startLatitude={}, startLongitude={}, avoidCrowds={}, topK={}",
                targetArea,
                preferredEventCategoryIds,
                requestedEventCategoryNames,
                searchStart,
                searchEnd,
                startLatitude,
                startLongitude,
                avoidCrowds,
                topK
        );

        boolean hasAreaConstraint = hasText(targetArea);
        boolean hasCategoryConstraint = !preferredEventCategoryIds.isEmpty()
                || !requestedEventCategoryNames.isEmpty();

        List<Event> candidateEvents;

        if (!hasAreaConstraint) {
            candidateEvents = eventRepository.findAllEventsByDate(searchStart, searchEnd);

            log.info(
                    "[행사 추천 후보 1단계] 지역 조건 없음: 전체지역+기간으로 조회, date={}~{}, candidateCount={}",
                    searchStart,
                    searchEnd,
                    candidateEvents.size()
            );
        } else {
            Map<Long, Event> candidateEventMap = new LinkedHashMap<>();

            // 취향 매핑으로 얻은 EVENT 카테고리 ID 기준 후보
            if (!preferredEventCategoryIds.isEmpty()) {
                eventRepository.findRecommendedEventsWithCategoryIds(
                                targetArea,
                                searchStart,
                                searchEnd,
                                preferredEventCategoryIds
                        )
                        .forEach(event -> candidateEventMap.put(event.getId(), event));
            }

            // 화면에서 직접 선택한 EVENT 카테고리명 기준 후보
            if (!requestedEventCategoryNames.isEmpty()) {
                eventRepository.findRecommendedEvents(targetArea, searchStart, searchEnd).stream()
                        .filter(event -> matchesRequestedCategory(
                                event,
                                Collections.emptySet(),
                                requestedEventCategoryNames
                        ))
                        .forEach(event -> candidateEventMap.put(event.getId(), event));
            }

            // 카테고리 조건이 없을 때만 지역 + 기간 전체 후보 조회
            if (!hasCategoryConstraint) {
                eventRepository.findRecommendedEvents(targetArea, searchStart, searchEnd)
                        .forEach(event -> candidateEventMap.put(event.getId(), event));
            }

            candidateEvents = new ArrayList<>(candidateEventMap.values());

            log.info(
                    "[행사 추천 후보 1단계] 조건=지역+기간+카테고리, area={}, date={}~{}, categoryIds={}, categoryNames={}, candidateCount={}",
                    targetArea,
                    searchStart,
                    searchEnd,
                    preferredEventCategoryIds,
                    requestedEventCategoryNames,
                    candidateEvents.size()
            );

            // 실제 카테고리 조건을 적용했을 때만 카테고리 해제 재조회
            if (hasCategoryConstraint && candidateEvents.isEmpty()) {
                candidateEvents = eventRepository.findRecommendedEvents(
                        targetArea,
                        searchStart,
                        searchEnd
                );

                log.info(
                        "[행사 추천 후보 2단계] 조건=지역+기간(카테고리 해제), area={}, date={}~{}, candidateCount={}",
                        targetArea,
                        searchStart,
                        searchEnd,
                        candidateEvents.size()
                );
            }

            if (candidateEvents.isEmpty()) {
                LocalDate expandedStart = searchStart.minusDays(7);
                LocalDate expandedEnd = searchEnd.plusDays(7);

                candidateEvents = eventRepository.findRecommendedEvents(
                        targetArea,
                        expandedStart,
                        expandedEnd
                );

                log.info(
                        "[행사 추천 후보 3단계] 조건=지역+확장기간, area={}, date={}~{}, candidateCount={}",
                        targetArea,
                        expandedStart,
                        expandedEnd,
                        candidateEvents.size()
                );
            }

            if (candidateEvents.isEmpty()) {
                candidateEvents = eventRepository.findAllEventsByDate(searchStart, searchEnd);

                log.info(
                        "[행사 추천 후보 4단계] 조건=전체지역+기간, date={}~{}, candidateCount={}",
                        searchStart,
                        searchEnd,
                        candidateEvents.size()
                );
            }
        }

        log.info("[행사 추천 후보 최종] selectedCandidateCount={}", candidateEvents.size());

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

        if (dayOfWeekFilteredEvents.isEmpty()) {
            log.info("요일 하드 필터링 결과 매칭되는 행사가 0건입니다. 선택 B 정책에 따라 요일 필터를 해제하고 차선책을 제공합니다.");
        } else {
            log.info("요일 하드 필터링 통과 완료. 남은 후보 개수: {}/{}", dayOfWeekFilteredEvents.size(), candidateEvents.size());
            candidateEvents = dayOfWeekFilteredEvents;
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
                    && matchesRequestedCategory(event, preferredEventCategoryIds, requestedEventCategoryNames)) {
                double currentScore = rrfScores.get(event.getId());
                rrfScores.put(event.getId(), currentScore * categoryBoost);
            }
        }

        return selectBeamSearchEventIds(
                candidateEvents,
                rrfScores,
                searchStart,
                startLatitude,
                startLongitude,
                avoidCrowds,
                topK,
                DEFAULT_BEAM_WIDTH
        );
    }

    /**
     * 여러 후보 경로를 동시에 유지하는 빔 서치 기반 행사 선택 로직입니다.
     * <p>
     * 각 단계에서 현재 빔의 모든 경로를 다음 행사 후보로 확장하고,
     * 누적 점수가 높은 상위 beamWidth개의 경로만 다음 단계에 유지합니다.
     * 이를 통해 각 단계의 단일 최고 점수를 즉시 확정하는 방식보다
     * 전체 코스의 선호도·혼잡도·이동 거리 조합을 함께 고려합니다.
     */
    private List<Long> selectBeamSearchEventIds(
            List<Event> candidateEvents,
            Map<Long, Double> preferenceScores,
            LocalDate searchStart,
            Double startLatitude,
            Double startLongitude,
            boolean avoidCrowds,
            int topK,
            int beamWidth
    ) {
        if (topK <= 0 || beamWidth <= 0 || candidateEvents.isEmpty()) {
            return Collections.emptyList();
        }

        double maxPreferenceScore = preferenceScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        List<EventCandidate> candidates = candidateEvents.stream()
                .filter(event -> preferenceScores.containsKey(event.getId()))
                .map(event -> new EventCandidate(
                        event,
                        normalizePreferenceScore(
                                preferenceScores.getOrDefault(event.getId(), 0.0),
                                maxPreferenceScore
                        ),
                        avoidCrowds ? calculateCrowdScore(event) : 0.0,
                        calculateWeatherScore(event, searchStart)
                ))
                .toList();

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<BeamState> beam = List.of(BeamState.initial(startLatitude, startLongitude));
        int maxDepth = Math.min(topK, candidates.size());

        for (int depth = 0; depth < maxDepth; depth++) {
            List<BeamState> expandedStates = new ArrayList<>();

            for (BeamState state : beam) {
                for (EventCandidate candidate : candidates) {
                    Long eventId = candidate.event().getId();
                    if (state.selectedEventIds().contains(eventId)) {
                        continue;
                    }

                    double candidateScore = calculateCandidateScore(
                            candidate,
                            state.currentLatitude(),
                            state.currentLongitude(),
                            avoidCrowds
                    );
                    expandedStates.add(state.extend(candidate.event(), candidateScore));
                }
            }

            if (expandedStates.isEmpty()) {
                break;
            }

            beam = expandedStates.stream()
                    .sorted(Comparator
                            .comparingDouble(BeamState::totalScore)
                            .reversed()
                            .thenComparing(BeamState::eventIds, this::compareEventIdSequences))
                    .limit(beamWidth)
                    .toList();
        }

        return beam.stream()
                .max(Comparator
                        .comparingDouble(BeamState::totalScore)
                        .thenComparing(BeamState::eventIds, this::compareEventIdSequences))
                .map(BeamState::eventIds)
                .orElseGet(Collections::emptyList);
    }

    private int compareEventIdSequences(List<Long> first, List<Long> second) {
        int size = Math.min(first.size(), second.size());
        for (int index = 0; index < size; index++) {
            int comparison = Long.compare(first.get(index), second.get(index));
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(first.size(), second.size());
    }

    private double calculateCandidateScore(
            EventCandidate candidate,
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
                    + (distanceScore * AVOID_CROWD_DISTANCE_WEIGHT)
                    + (candidate.weatherScore() * AVOID_CROWD_WEATHER_WEIGHT);
        }

        return (candidate.preferenceScore() * INDIFFERENT_PREFERENCE_WEIGHT)
                + (distanceScore * INDIFFERENT_DISTANCE_WEIGHT)
                + (candidate.weatherScore() * INDIFFERENT_WEATHER_WEIGHT);
    }

    private double calculateWeatherScore(Event event, LocalDate targetDate) {
        if (event == null || targetDate == null || !hasCoordinate(event)) {
            log.info(
                    "날씨 점수 중립 처리: eventId={}, targetDate={}, weatherScore={}",
                    event != null ? event.getId() : null,
                    targetDate,
                    DEFAULT_WEATHER_SCORE
            );
            return DEFAULT_WEATHER_SCORE;
        }

        WeatherCondition weatherCondition = weatherForecastService.getRepresentativeForecast(
                        targetDate,
                        event.getLatitude(),
                        event.getLongitude()
                )
                .map(weatherConditionClassifier::classify)
                .orElse(WeatherCondition.UNKNOWN);

        Boolean indoorEvent = eventIndoorOutdoorPolicy.isIndoor(event);
        double weatherScore = weatherScoreCalculator.calculate(weatherCondition, indoorEvent);

        log.info(
                "행사 위치 기준 날씨 점수 계산: eventId={}, title={}, latitude={}, longitude={}, indoor={}, condition={}, weatherScore={}",
                event.getId(),
                event.getTitle(),
                event.getLatitude(),
                event.getLongitude(),
                indoorEvent,
                weatherCondition,
                weatherScore
        );

        return weatherScore;
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

    private record EventCandidate(
            Event event,
            double preferenceScore,
            double crowdScore,
            double weatherScore
    ) {
    }

    private record BeamState(
            List<Long> eventIds,
            Set<Long> selectedEventIds,
            Double currentLatitude,
            Double currentLongitude,
            double totalScore
    ) {
        private static BeamState initial(Double startLatitude, Double startLongitude) {
            return new BeamState(
                    List.of(),
                    Set.of(),
                    startLatitude,
                    startLongitude,
                    0.0
            );
        }

        private BeamState extend(Event event, double candidateScore) {
            List<Long> nextEventIds = new ArrayList<>(eventIds);
            nextEventIds.add(event.getId());

            Set<Long> nextSelectedEventIds = new HashSet<>(selectedEventIds);
            nextSelectedEventIds.add(event.getId());

            Double nextLatitude = currentLatitude;
            Double nextLongitude = currentLongitude;
            if (event.getLatitude() != null && event.getLongitude() != null) {
                nextLatitude = event.getLatitude();
                nextLongitude = event.getLongitude();
            }

            return new BeamState(
                    List.copyOf(nextEventIds),
                    Set.copyOf(nextSelectedEventIds),
                    nextLatitude,
                    nextLongitude,
                    totalScore + candidateScore
            );
        }
    }

    public record RecommendedCourseDraft(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            String baseArea,
            String companionType,
            double latitude,
            double longitude,
            List<RecommendedEvent> events
    ) {
    }

    public record RecommendedEvent(
            Long eventId,
            String reason,
            int visitOrder
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

    private boolean matchesRequestedCategory(
            Event event,
            Set<Long> preferredEventCategoryIds,
            Set<String> requestedEventCategoryNames
    ) {
        if (event == null || event.getCategory() == null) {
            return false;
        }

        if (preferredEventCategoryIds.contains(event.getCategory().getId())) {
            return true;
        }

        return requestedEventCategoryNames.contains(event.getCategory().getName());
    }

    private String normalizeRecommendationArea(String area) {
        if (!hasText(area)) {
            return area;
        }

        String normalizedArea = area.trim()
                .replace("서울특별시", "")
                .replace("서울시", "")
                .trim();

        if (normalizedArea.contains("성수")
                || normalizedArea.contains("서울숲")
                || normalizedArea.contains("뚝섬")) {
            return "성동구";
        }

        if (normalizedArea.contains("홍대")
                || normalizedArea.contains("연남")
                || normalizedArea.contains("합정")) {
            return "마포구";
        }

        if (normalizedArea.contains("광화문")
                || normalizedArea.contains("경복궁")
                || normalizedArea.contains("인사동")) {
            return "종로구";
        }

        return normalizedArea;
    }
}