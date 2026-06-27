package come.back.gotoday.recommend.service;

import come.back.gotoday.category.repository.PreferenceEventCategoryMappingRepository;
import come.back.gotoday.category.repository.PreferenceTourCategoryMappingRepository;
import come.back.gotoday.tour.entity.Tour;
import come.back.gotoday.tour.repository.TourRepository;

import come.back.gotoday.crowd.service.CrowdScoreCalculator;
import come.back.gotoday.crowd.service.NearestCrowdAreaService;
import come.back.gotoday.crowd.util.GeoDistanceCalculator;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.event.service.EventScheduleMatcher;
import come.back.gotoday.external.ai.service.AiRecommendationReasonService;
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
    private static final List<Double> LOCATION_SEARCH_RADII_KM = List.of(3.0, 5.0, 8.0);

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserPreferenceCategoryRepository userPreferenceCategoryRepository;
    private final PreferenceEventCategoryMappingRepository preferenceEventCategoryMappingRepository;
    private final PreferenceTourCategoryMappingRepository preferenceTourCategoryMappingRepository;
    private final EventRepository eventRepository;
    private final TourRepository tourRepository;
    private final NearestCrowdAreaService nearestCrowdAreaService;
    private final CrowdScoreCalculator crowdScoreCalculator;

    private final EventScheduleMatcher eventScheduleMatcher;
    private final SearchUtils searchUtils;
    private final VectorEmbeddingEngine vectorEngine;
    private final WeatherForecastService weatherForecastService;
    private final WeatherConditionClassifier weatherConditionClassifier;
    private final WeatherScoreCalculator weatherScoreCalculator;
    private final EventIndoorOutdoorPolicy eventIndoorOutdoorPolicy;
    private final AiRecommendationReasonService aiRecommendationReasonService;

    public RecommendationService(UserPreferenceRepository userPreferenceRepository,
                                 UserPreferenceCategoryRepository userPreferenceCategoryRepository,
                                 PreferenceEventCategoryMappingRepository preferenceEventCategoryMappingRepository,
                                 PreferenceTourCategoryMappingRepository preferenceTourCategoryMappingRepository,
                                 EventRepository eventRepository,
                                 TourRepository tourRepository,
                                 NearestCrowdAreaService nearestCrowdAreaService,
                                 CrowdScoreCalculator crowdScoreCalculator,
                                 EventScheduleMatcher eventScheduleMatcher,
                                 SearchUtils searchUtils,
                                 VectorEmbeddingEngine vectorEngine,
                                 WeatherForecastService weatherForecastService,
                                 WeatherConditionClassifier weatherConditionClassifier,
                                 WeatherScoreCalculator weatherScoreCalculator,
                                 EventIndoorOutdoorPolicy eventIndoorOutdoorPolicy,
                                 AiRecommendationReasonService aiRecommendationReasonService) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userPreferenceCategoryRepository = userPreferenceCategoryRepository;
        this.preferenceEventCategoryMappingRepository = preferenceEventCategoryMappingRepository;
        this.preferenceTourCategoryMappingRepository = preferenceTourCategoryMappingRepository;
        this.eventRepository = eventRepository;
        this.tourRepository = tourRepository;
        this.nearestCrowdAreaService = nearestCrowdAreaService;
        this.crowdScoreCalculator = crowdScoreCalculator;
        this.eventScheduleMatcher = eventScheduleMatcher;
        this.searchUtils = searchUtils;
        this.vectorEngine = vectorEngine;
        this.weatherForecastService = weatherForecastService;
        this.weatherConditionClassifier = weatherConditionClassifier;
        this.weatherScoreCalculator = weatherScoreCalculator;
        this.eventIndoorOutdoorPolicy = eventIndoorOutdoorPolicy;
        this.aiRecommendationReasonService = aiRecommendationReasonService;
    }
    @Transactional(readOnly = true)
    public RecommendationCandidateDraft recommendCandidates(
            Long memberId,
            RecommendationCourseCreateRequest request
    ) {
        var preferenceOptional = userPreferenceRepository.findByMemberId(memberId);

        List<String> savedCategories = preferenceOptional
                .map(preference -> userPreferenceCategoryRepository.findCategoryNamesByPreferenceId(preference.getId()))
                .orElseGet(Collections::emptyList);

        List<String> selectedCategories = request.categories() != null && !request.categories().isEmpty()
                ? request.categories().stream()
                        .filter(this::hasText)
                        .map(String::trim)
                        .toList()
                : savedCategories;

        String rawSelectedArea = hasText(request.area())
                ? request.area()
                : preferenceOptional.map(preference -> preference.getPreferredArea()).orElse(null);
        String selectedArea = normalizeRecommendationArea(rawSelectedArea);

        String selectedCompanionType = hasText(request.companionType())
                ? request.companionType()
                : preferenceOptional
                        .map(preference -> preference.getCompanionType() != null
                                ? preference.getCompanionType().toString()
                                : null)
                        .orElse(null);

        Set<Long> preferredEventCategoryIds = selectedCategories.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(preferenceEventCategoryMappingRepository
                        .findEventCategoryIdsByPreferenceCategoryNames(selectedCategories));

        Set<String> requestedEventCategoryNames = request.categories() == null
                ? Collections.emptySet()
                : request.categories().stream()
                        .filter(this::hasText)
                        .map(String::trim)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        boolean avoidCrowds = preferenceOptional
                .map(preference -> Boolean.TRUE.equals(preference.getAvoidCrowded()))
                .orElse(false);

        String queryText = createQueryText(
                selectedArea,
                String.join(", ", selectedCategories),
                selectedCompanionType
        );

        List<RankedEventCandidate> rankedEvents = rankRecommendedEvents(
                selectedArea,
                preferredEventCategoryIds,
                requestedEventCategoryNames,
                queryText,
                request.startDate(),
                request.endDate(),
                request.latitude(),
                request.longitude(),
                avoidCrowds,
                10
        );

        List<RecommendationCandidate> eventCandidates = createEventCandidates(
                rankedEvents,
                selectedArea,
                preferredEventCategoryIds,
                requestedEventCategoryNames
        );

        List<String> tourCat3Codes = selectedCategories.isEmpty()
                ? Collections.emptyList()
                : preferenceTourCategoryMappingRepository
                        .findTourCat3CodesByPreferenceCategoryNames(selectedCategories);

        List<RecommendationCandidate> tourCandidates = createTourCandidates(
                tourCat3Codes,
                queryText,
                request.latitude(),
                request.longitude()
        );

        List<RecommendationCandidate> candidates = new ArrayList<>(eventCandidates.size() + tourCandidates.size());
        candidates.addAll(eventCandidates);
        candidates.addAll(tourCandidates);

        List<RecommendationCandidate> topCandidates = candidates.stream()
                .sorted(Comparator
                        .comparingDouble(RecommendationCandidate::score)
                        .reversed()
                        .thenComparing(
                                RecommendationCandidate::title,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ))
                .limit(10)
                .toList();

        if (topCandidates.isEmpty()) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_EVENT_NOT_FOUND);
        }

        List<String> placeRecommendationReasons = aiRecommendationReasonService.generatePlaceReasons(
                topCandidates.stream()
                        .map(candidate -> new AiRecommendationReasonService.PlaceReasonContext(
                                request.startDate(),
                                rawSelectedArea,
                                selectedCompanionType,
                                selectedCategories,
                                candidate.title(),
                                candidate.type().name(),
                                candidate.detailCategoryName() != null
                                        ? candidate.detailCategoryName()
                                        : candidate.categoryName()
                        ))
                        .toList()
        );

        List<RecommendationCandidate> candidatesWithAiReasons = new ArrayList<>(topCandidates.size());
        for (int index = 0; index < topCandidates.size(); index++) {
            RecommendationCandidate candidate = topCandidates.get(index);
            candidatesWithAiReasons.add(new RecommendationCandidate(
                    candidate.type(),
                    candidate.eventId(),
                    candidate.tourId(),
                    candidate.title(),
                    candidate.categoryName(),
                    candidate.detailCategoryName(),
                    candidate.address(),
                    candidate.latitude(),
                    candidate.longitude(),
                    candidate.score(),
                    placeRecommendationReasons.get(index)
            ));
        }

        log.info(
                "[통합 추천 후보 완료] memberId={}, eventCount={}, tourCount={}, topCandidateCount={}",
                memberId,
                eventCandidates.size(),
                tourCandidates.size(),
                candidatesWithAiReasons.size()
        );

        String courseRecommendationReason = aiRecommendationReasonService.generateCourseReason(
                new AiRecommendationReasonService.CourseReasonContext(
                        request.startDate(),
                        rawSelectedArea,
                        selectedCompanionType,
                        selectedCategories,
                        candidatesWithAiReasons.stream()
                                .map(RecommendationCandidate::title)
                                .toList()
                )
        );
        return new RecommendationCandidateDraft(
                request.startDate(),
                request.endDate(),
                selectedArea,
                selectedCompanionType,
                candidatesWithAiReasons,
                courseRecommendationReason
        );
    }

    private List<RecommendationCandidate> createEventCandidates(
            List<RankedEventCandidate> rankedEvents,
            String selectedArea,
            Set<Long> preferredEventCategoryIds,
            Set<String> requestedEventCategoryNames
    ) {
        if (rankedEvents.isEmpty()) {
            return Collections.emptyList();
        }

        return rankedEvents.stream()
                .map(rankedEvent -> {
                    Event event = rankedEvent.event();
                    return new RecommendationCandidate(
                            CandidateType.EVENT,
                            event.getId(),
                            null,
                            event.getTitle(),
                            event.getCategory() != null ? event.getCategory().getName() : null,
                            event.getEventCategory(),
                            event.getPlace() != null ? event.getPlace().getAddress() : event.getArea(),
                            event.getLatitude(),
                            event.getLongitude(),
                            rankedEvent.score(),
                            createRecommendationReason(
                                    event,
                                    selectedArea,
                                    preferredEventCategoryIds,
                                    requestedEventCategoryNames
                            )
                    );
                })
                .toList();
    }

    private List<RecommendationCandidate> createTourCandidates(
            List<String> tourCat3Codes,
            String queryText,
            Double startLatitude,
            Double startLongitude
    ) {
        if (tourCat3Codes == null || tourCat3Codes.isEmpty()
                || startLatitude == null || startLongitude == null) {
            return Collections.emptyList();
        }

        for (double radiusKm : LOCATION_SEARCH_RADII_KM) {
            List<Tour> tours = findToursWithinRadius(
                    tourCat3Codes,
                    startLatitude,
                    startLongitude,
                    radiusKm
            );

            if (!tours.isEmpty()) {
                return rankTourCandidates(tours, queryText, startLatitude, startLongitude);
            }
        }

        return Collections.emptyList();
    }

    private List<RecommendationCandidate> rankTourCandidates(
            List<Tour> tours,
            String queryText,
            double startLatitude,
            double startLongitude
    ) {
        List<String> queryTokens = searchUtils.tokenize(queryText);
        float[] queryEmbedding = vectorEngine.getEmbedding(queryText);

        Map<Long, List<String>> docTokensMap = new HashMap<>();
        Map<Long, float[]> docEmbeddingMap = new HashMap<>();

        for (Tour tour : tours) {
            String docText = String.format(
                    "[지역: %s] [카테고리: %s] [세부분류: %s] [관광지명: %s] [주소: %s] [소개: %s]",
                    tour.getArea(),
                    tour.getCategory() != null ? tour.getCategory().getName() : "관광지",
                    tour.getDetailCategoryName(),
                    tour.getTitle(),
                    tour.getAddress(),
                    tour.getOverview()
            );
            docTokensMap.put(tour.getId(), searchUtils.tokenize(docText));

            float[] embeddingVector = tour.getEmbeddingVector();
            if (embeddingVector != null && embeddingVector.length > 0) {
                docEmbeddingMap.put(tour.getId(), embeddingVector);
            }
        }

        Map<Long, Double> bm25Scores = searchUtils.calculateBM25(queryTokens, docTokensMap);
        Map<Long, Double> denseScores = new HashMap<>();
        for (Map.Entry<Long, float[]> entry : docEmbeddingMap.entrySet()) {
            denseScores.put(
                    entry.getKey(),
                    searchUtils.cosineSimilarity(queryEmbedding, entry.getValue())
            );
        }

        List<Long> bm25Ranked = bm25Scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        List<Long> denseRanked = denseScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, Double> rrfScores = new HashMap<>();
        int rrfK = 60;
        double bm25Weight = 0.3;
        double denseWeight = 0.7;

        for (int rank = 0; rank < bm25Ranked.size(); rank++) {
            Long tourId = bm25Ranked.get(rank);
            double score = 1.0 / (rrfK + rank + 1);
            rrfScores.merge(tourId, score * bm25Weight, Double::sum);
        }

        for (int rank = 0; rank < denseRanked.size(); rank++) {
            Long tourId = denseRanked.get(rank);
            double score = 1.0 / (rrfK + rank + 1);
            rrfScores.merge(tourId, score * denseWeight, Double::sum);
        }

        double maxPreferenceScore = rrfScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        return tours.stream()
                .map(tour -> {
                    double preferenceScore = normalizePreferenceScore(
                            rrfScores.getOrDefault(tour.getId(), 0.0),
                            maxPreferenceScore
                    );
                    double distanceKm = GeoDistanceCalculator.calculateKilometers(
                            startLatitude,
                            startLongitude,
                            tour.getLatitude(),
                            tour.getLongitude()
                    );
                    double distanceScore = 1.0 / (1.0 + distanceKm);
                    double totalScore = (preferenceScore * 0.7) + (distanceScore * 0.3);

                    return new RecommendationCandidate(
                            CandidateType.TOUR,
                            null,
                            tour.getId(),
                            tour.getTitle(),
                            tour.getCategory() != null ? tour.getCategory().getName() : "관광지",
                            tour.getDetailCategoryName(),
                            tour.getAddress(),
                            tour.getLatitude(),
                            tour.getLongitude(),
                            totalScore,
                            "선택한 취향과 관광지 정보의 유사도, 출발 위치와의 거리를 함께 고려해 추천되었습니다."
                    );
                })
                .sorted(Comparator
                        .comparingDouble(RecommendationCandidate::score)
                        .reversed()
                        .thenComparing(RecommendationCandidate::tourId))
                .toList();
    }

    private List<Tour> findToursWithinRadius(
            List<String> tourCat3Codes,
            double latitude,
            double longitude,
            double radiusKm
    ) {
        double latitudeDelta = radiusKm / 111.0;
        double longitudeDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));

        return tourRepository.findActiveToursByCat3WithinBounds(
                        tourCat3Codes,
                        latitude - latitudeDelta,
                        latitude + latitudeDelta,
                        longitude - longitudeDelta,
                        longitude + longitudeDelta
                ).stream()
                .filter(tour -> tour.getLatitude() != null && tour.getLongitude() != null)
                .filter(tour -> GeoDistanceCalculator.calculateKilometers(
                        latitude,
                        longitude,
                        tour.getLatitude(),
                        tour.getLongitude()
                ) <= radiusKm)
                .toList();
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
        return rankRecommendedEvents(
                targetArea,
                preferredEventCategoryIds,
                requestedEventCategoryNames,
                queryText,
                searchStart,
                searchEnd,
                startLatitude,
                startLongitude,
                avoidCrowds,
                topK
        ).stream()
                .map(rankedEvent -> rankedEvent.event().getId())
                .toList();
    }

    private List<RankedEventCandidate> rankRecommendedEvents(
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
        boolean hasStartCoordinate = startLatitude != null && startLongitude != null;

        List<Event> candidateEvents;
        if (hasStartCoordinate) {
            candidateEvents = findCoordinateBasedCandidateEvents(
                    targetArea,
                    preferredEventCategoryIds,
                    requestedEventCategoryNames,
                    searchStart,
                    searchEnd,
                    startLatitude,
                    startLongitude,
                    hasCategoryConstraint
            );
        } else {
            candidateEvents = findAreaBasedCandidateEvents(
                    targetArea,
                    preferredEventCategoryIds,
                    requestedEventCategoryNames,
                    searchStart,
                    searchEnd,
                    hasAreaConstraint,
                    hasCategoryConstraint
            );
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
                .toList();

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

        for (Event event : candidateEvents) {
            String docText = String.format(
                    "[지역: %s] [카테고리: %s] [세부분류: %s] [타겟/대상: %s] [행사명: %s]",
                    event.getArea(),
                    event.getCategory().getName(),
                    event.getEventCategory(),
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
        for (Map.Entry<Long, float[]> entry : docEmbeddingMap.entrySet()) {
            denseScores.put(entry.getKey(), searchUtils.cosineSimilarity(queryEmbedding, entry.getValue()));
        }

        List<Long> bm25Ranked = bm25Scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
        List<Long> denseRanked = denseScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, Double> rrfScores = new HashMap<>();
        int rrfK = 60;
        double denseWeight = 0.7;
        double bm25Weight = 0.3;
        double categoryBoost = 2.0;

        for (int rank = 0; rank < bm25Ranked.size(); rank++) {
            Long eventId = bm25Ranked.get(rank);
            rrfScores.merge(eventId, (1.0 / (rrfK + rank + 1)) * bm25Weight, Double::sum);
        }
        for (int rank = 0; rank < denseRanked.size(); rank++) {
            Long eventId = denseRanked.get(rank);
            rrfScores.merge(eventId, (1.0 / (rrfK + rank + 1)) * denseWeight, Double::sum);
        }
        for (Event event : candidateEvents) {
            if (rrfScores.containsKey(event.getId())
                    && matchesRequestedCategory(event, preferredEventCategoryIds, requestedEventCategoryNames)) {
                rrfScores.computeIfPresent(event.getId(), (eventId, score) -> score * categoryBoost);
            }
        }

        double maxPreferenceScore = rrfScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        return candidateEvents.stream()
                .filter(event -> rrfScores.containsKey(event.getId()))
                .map(event -> {
                    EventCandidate candidate = new EventCandidate(
                            event,
                            normalizePreferenceScore(rrfScores.getOrDefault(event.getId(), 0.0), maxPreferenceScore),
                            avoidCrowds ? calculateCrowdScore(event) : 0.0,
                            calculateWeatherScore(event, searchStart)
                    );
                    return new RankedEventCandidate(
                            event,
                            calculateCandidateScore(candidate, startLatitude, startLongitude, avoidCrowds)
                    );
                })
                .sorted(Comparator
                        .comparingDouble(RankedEventCandidate::score)
                        .reversed()
                        .thenComparing(rankedEvent -> rankedEvent.event().getId()))
                .limit(topK)
                .toList();
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

    private record RankedEventCandidate(
            Event event,
            double score
    ) {
    }


    public enum CandidateType {
        EVENT,
        TOUR
    }

    public record RecommendationCandidateDraft(
            LocalDate startDate,
            LocalDate endDate,
            String baseArea,
            String companionType,
            List<RecommendationCandidate> candidates,
            String recommendationReason
    ) {
        public RecommendationCandidateDraft(
                LocalDate startDate,
                LocalDate endDate,
                String baseArea,
                String companionType,
                List<RecommendationCandidate> candidates
        ) {
            this(startDate, endDate, baseArea, companionType, candidates, null);
        }
    }

    public record RecommendationCandidate(
            CandidateType type,
            Long eventId,
            Long tourId,
            String title,
            String categoryName,
            String detailCategoryName,
            String address,
            Double latitude,
            Double longitude,
            double score,
            String recommendationReason
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

        if (containsAny(normalizedArea, "성수", "서울숲", "뚝섬", "왕십리")) {
            return "성동구";
        }

        if (containsAny(normalizedArea, "홍대", "연남", "합정", "상수", "망원", "월드컵공원")) {
            return "마포구";
        }

        if (containsAny(normalizedArea, "광화문", "경복궁", "인사동", "북촌", "삼청동", "익선동", "종묘", "창덕궁", "대학로")) {
            return "종로구";
        }

        if (containsAny(normalizedArea, "명동", "을지로", "남산", "서울역", "충무로", "동대문디자인플라자", "DDP")) {
            return "중구";
        }

        if (containsAny(normalizedArea, "여의도", "더현대", "국회의사당", "63빌딩", "여의도한강공원")) {
            return "영등포구";
        }

        if (containsAny(normalizedArea, "잠실", "석촌", "송리단길", "롯데월드", "올림픽공원")) {
            return "송파구";
        }

        if (containsAny(normalizedArea, "이태원", "한남", "용산", "남영", "효창공원")) {
            return "용산구";
        }

        if (containsAny(normalizedArea, "강남", "역삼", "선릉", "삼성", "코엑스", "압구정", "청담", "신사", "가로수길")) {
            return "강남구";
        }

        if (containsAny(normalizedArea, "서초", "교대", "강남역", "반포", "고속터미널", "예술의전당")) {
            return "서초구";
        }

        if (containsAny(normalizedArea, "건대", "뚝섬유원지", "어린이대공원", "광진")) {
            return "광진구";
        }

        if (containsAny(normalizedArea, "신촌", "이대", "연세", "서대문")) {
            return "서대문구";
        }

        if (containsAny(normalizedArea, "노원", "상계", "중계", "태릉")) {
            return "노원구";
        }

        if (containsAny(normalizedArea, "강북", "수유", "미아", "북한산")) {
            return "강북구";
        }

        if (containsAny(normalizedArea, "도봉", "창동", "쌍문")) {
            return "도봉구";
        }

        if (containsAny(normalizedArea, "은평", "불광", "연신내", "응암")) {
            return "은평구";
        }

        if (containsAny(normalizedArea, "강서", "마곡", "김포공항", "화곡")) {
            return "강서구";
        }

        if (containsAny(normalizedArea, "양천", "목동", "신정")) {
            return "양천구";
        }

        if (containsAny(normalizedArea, "구로", "신도림", "가산", "독산", "금천")) {
            return normalizedArea.contains("가산") || normalizedArea.contains("독산") || normalizedArea.contains("금천")
                    ? "금천구"
                    : "구로구";
        }

        if (containsAny(normalizedArea, "동작", "사당", "흑석", "노량진", "보라매")) {
            return "동작구";
        }

        if (containsAny(normalizedArea, "관악", "신림", "서울대")) {
            return "관악구";
        }

        if (containsAny(normalizedArea, "동대문", "청량리", "회기", "장안")) {
            return "동대문구";
        }

        if (containsAny(normalizedArea, "중랑", "면목", "상봉", "망우")) {
            return "중랑구";
        }

        if (containsAny(normalizedArea, "성북", "성신여대", "길음", "정릉")) {
            return "성북구";
        }

        return normalizedArea;
    }

    private boolean containsAny(String value, String... keywords) {
        return Arrays.stream(keywords).anyMatch(value::contains);
    }

    private List<Event> findCoordinateBasedCandidateEvents(
            String targetArea,
            Set<Long> preferredEventCategoryIds,
            Set<String> requestedEventCategoryNames,
            LocalDate searchStart,
            LocalDate searchEnd,
            double startLatitude,
            double startLongitude,
            boolean hasCategoryConstraint
    ) {
        List<Event> dateCandidates = eventRepository.findAllEventsByDate(searchStart, searchEnd);

        List<Event> categoryCandidates = dateCandidates.stream()
                .filter(event -> !hasCategoryConstraint || matchesRequestedCategory(
                        event,
                        preferredEventCategoryIds,
                        requestedEventCategoryNames
                ))
                .toList();

        for (double radiusKm : LOCATION_SEARCH_RADII_KM) {
            List<Event> radiusCandidates = categoryCandidates.stream()
                    .filter(this::hasCoordinate)
                    .filter(event -> GeoDistanceCalculator.calculateKilometers(
                            startLatitude,
                            startLongitude,
                            event.getLatitude(),
                            event.getLongitude()
                    ) <= radiusKm)
                    .toList();

            log.info(
                    "[행사 추천 후보 좌표 단계] 조건=출발좌표+반경+기간+카테고리, radiusKm={}, date={}~{}, categoryIds={}, categoryNames={}, candidateCount={}",
                    radiusKm,
                    searchStart,
                    searchEnd,
                    preferredEventCategoryIds,
                    requestedEventCategoryNames,
                    radiusCandidates.size()
            );

            if (!radiusCandidates.isEmpty()) {
                return radiusCandidates;
            }
        }

        if (!categoryCandidates.isEmpty()) {
            log.info(
                    "[행사 추천 후보 좌표 fallback] 반경 내 후보 없음: 기간+카테고리 후보 사용, date={}~{}, candidateCount={}",
                    searchStart,
                    searchEnd,
                    categoryCandidates.size()
            );
            return categoryCandidates;
        }

        if (hasCategoryConstraint) {
            List<Event> areaCandidates = hasText(targetArea)
                    ? eventRepository.findRecommendedEvents(targetArea, searchStart, searchEnd)
                    : dateCandidates;

            log.info(
                    "[행사 추천 후보 좌표 fallback] 카테고리 후보 없음: 지역+기간 또는 전체기간 후보 사용, area={}, date={}~{}, candidateCount={}",
                    targetArea,
                    searchStart,
                    searchEnd,
                    areaCandidates.size()
            );
            return areaCandidates;
        }

        return dateCandidates;
    }

    private List<Event> findAreaBasedCandidateEvents(
            String targetArea,
            Set<Long> preferredEventCategoryIds,
            Set<String> requestedEventCategoryNames,
            LocalDate searchStart,
            LocalDate searchEnd,
            boolean hasAreaConstraint,
            boolean hasCategoryConstraint
    ) {
        if (!hasAreaConstraint) {
            List<Event> candidateEvents = eventRepository.findAllEventsByDate(searchStart, searchEnd);

            log.info(
                    "[행사 추천 후보 1단계] 지역·좌표 조건 없음: 전체지역+기간으로 조회, date={}~{}, candidateCount={}",
                    searchStart,
                    searchEnd,
                    candidateEvents.size()
            );

            return candidateEvents;
        }

        Map<Long, Event> candidateEventMap = new LinkedHashMap<>();

        if (!preferredEventCategoryIds.isEmpty()) {
            eventRepository.findRecommendedEventsWithCategoryIds(
                            targetArea,
                            searchStart,
                            searchEnd,
                            preferredEventCategoryIds
                    )
                    .forEach(event -> candidateEventMap.put(event.getId(), event));
        }

        if (!requestedEventCategoryNames.isEmpty()) {
            eventRepository.findRecommendedEvents(targetArea, searchStart, searchEnd).stream()
                    .filter(event -> matchesRequestedCategory(
                            event,
                            Collections.emptySet(),
                            requestedEventCategoryNames
                    ))
                    .forEach(event -> candidateEventMap.put(event.getId(), event));
        }

        if (!hasCategoryConstraint) {
            eventRepository.findRecommendedEvents(targetArea, searchStart, searchEnd)
                    .forEach(event -> candidateEventMap.put(event.getId(), event));
        }

        List<Event> candidateEvents = new ArrayList<>(candidateEventMap.values());

        log.info(
                "[행사 추천 후보 지역 단계] 조건=지역+기간+카테고리, area={}, date={}~{}, categoryIds={}, categoryNames={}, candidateCount={}",
                targetArea,
                searchStart,
                searchEnd,
                preferredEventCategoryIds,
                requestedEventCategoryNames,
                candidateEvents.size()
        );

        if (hasCategoryConstraint && candidateEvents.isEmpty()) {
            candidateEvents = eventRepository.findRecommendedEvents(targetArea, searchStart, searchEnd);

            log.info(
                    "[행사 추천 후보 지역 fallback] 조건=지역+기간(카테고리 해제), area={}, date={}~{}, candidateCount={}",
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
                    "[행사 추천 후보 지역 fallback] 조건=지역+확장기간, area={}, date={}~{}, candidateCount={}",
                    targetArea,
                    expandedStart,
                    expandedEnd,
                    candidateEvents.size()
            );
        }

        if (candidateEvents.isEmpty()) {
            candidateEvents = eventRepository.findAllEventsByDate(searchStart, searchEnd);

            log.info(
                    "[행사 추천 후보 지역 fallback] 조건=전체지역+기간, date={}~{}, candidateCount={}",
                    searchStart,
                    searchEnd,
                    candidateEvents.size()
            );
        }

        return candidateEvents;
    }

    private boolean hasCoordinate(Double latitude, Double longitude) {
        return latitude != null && longitude != null;
    }
}