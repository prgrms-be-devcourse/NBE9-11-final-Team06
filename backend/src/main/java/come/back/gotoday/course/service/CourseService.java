package come.back.gotoday.course.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.course.dto.*;
import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.entity.CoursePlace;
import come.back.gotoday.course.entity.SavedCourse;
import come.back.gotoday.course.repository.CoursePlaceRepository;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.course.repository.SavedCourseRepository;
import come.back.gotoday.course.type.CourseItemType;
import come.back.gotoday.course.type.RestaurantType;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.external.ai.service.AiRecommendationReasonService;
import come.back.gotoday.external.kakao.dto.KakaoPlaceDocument;
import come.back.gotoday.external.kakao.dto.KakaoPlaceResponse;
import come.back.gotoday.external.kakao.service.KakaoLocalService;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import come.back.gotoday.place.service.PlaceService;
import come.back.gotoday.recommend.service.RecommendationService;
import come.back.gotoday.tour.entity.Tour;
import come.back.gotoday.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private static final int SAVED_COURSE_PAGE_SIZE = 10;
    private static final int DEFAULT_PREVIEW_EVENT_COUNT = 3;
    private static final double NEARBY_PLACE_RADIUS_METER = 500.0;
    private static final int DEFAULT_ROUTE_BEAM_WIDTH = 5;
    private static final String DEFAULT_COURSE_RECOMMENDATION_REASON =
            "선택한 장소와 이동 동선을 고려해 구성한 추천 코스입니다.";
    private static final String DEFAULT_PLACE_RECOMMENDATION_REASON =
            "선택한 일정과 가까운 위치를 기준으로 추천된 장소입니다.";

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final SavedCourseRepository savedCourseRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;
    private final TourRepository tourRepository;
    private final CategoryRepository categoryRepository;

    private final RecommendationService recommendationService;
    private final EventRepository eventRepository;
    private final KakaoLocalService kakaoLocalService;
    private final PlaceService placeService;
    private final AiRecommendationReasonService aiRecommendationReasonService;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long createCourse(Long memberId, CourseCreateRequest request) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        CourseCreationContext creationContext = transactionTemplate.execute(
                status -> prepareCourseCreation(memberId, request)
        );

        if (creationContext == null) {
            throw new IllegalStateException("코스 생성 준비에 실패했습니다.");
        }

        String finalCourseRecommendationReason = generateFinalCourseRecommendationReason(
                memberId,
                creationContext
        );

        List<String> finalPlaceRecommendationReasons = generateFinalPlaceRecommendationReasons(
                creationContext.orderedPlaces()
        );

        Long courseId = transactionTemplate.execute(status -> saveCourse(
                memberId,
                creationContext,
                finalCourseRecommendationReason,
                finalPlaceRecommendationReasons
        ));

        if (courseId == null) {
            throw new IllegalStateException("코스 저장에 실패했습니다.");
        }

        return courseId;
    }

    private String generateFinalCourseRecommendationReason(
            Long memberId,
            CourseCreationContext creationContext
    ) {
        try {
            return aiRecommendationReasonService.generateFinalCourseReason(
                    new AiRecommendationReasonService.FinalCourseReasonContext(
                            creationContext.request().startDate(),
                            creationContext.request().baseArea(),
                            creationContext.request().companionType(),
                            List.of(),
                            creationContext.orderedPlaces().stream()
                                    .map(this::getCandidateName)
                                    .toList()
                    )
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "AI 최종 코스 추천 이유 생성에 실패해 기본 문구를 사용합니다. memberId={}",
                    memberId,
                    exception
            );
            return DEFAULT_COURSE_RECOMMENDATION_REASON;
        }
    }

    private List<String> generateFinalPlaceRecommendationReasons(
            List<CourseRouteCandidate> orderedPlaces
    ) {
        try {
            return aiRecommendationReasonService.generateFinalPlaceReasons(
                    buildFinalPlaceReasonContexts(orderedPlaces)
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "AI 장소별 추천 이유 생성에 실패해 기본 문구를 사용합니다. placeCount={}",
                    orderedPlaces.size(),
                    exception
            );
            return java.util.Collections.nCopies(
                    orderedPlaces.size(),
                    DEFAULT_PLACE_RECOMMENDATION_REASON
            );
        }
    }

    private CourseCreationContext prepareCourseCreation(Long memberId, CourseCreateRequest request) {
        getMemberOrThrow(memberId);

        List<Event> events = findEventsByIds(request.eventIds());
        List<Tour> tours = findToursByIds(request.tourIds());
        Place restaurant = request.restaurantId() == null
                ? null
                : findPlaceById(request.restaurantId());
        Place cafe = request.cafeId() == null
                ? null
                : findPlaceById(request.cafeId());

        if (restaurant != null && cafe != null && restaurant.getId().equals(cafe.getId())) {
            throw new IllegalArgumentException("식당과 카페에 같은 장소를 선택할 수 없습니다.");
        }

        Map<Long, String> eventRecommendationReasons = request.getSelectedRecommendationItemsOrEmpty()
                .stream()
                .filter(CourseCreateRequest.SelectedRecommendationItem::isEvent)
                .filter(item -> item.recommendationReason() != null && !item.recommendationReason().isBlank())
                .collect(Collectors.toMap(
                        CourseCreateRequest.SelectedRecommendationItem::eventId,
                        item -> item.recommendationReason().trim(),
                        (first, ignored) -> first
                ));

        Map<Long, String> tourRecommendationReasons = request.getSelectedRecommendationItemsOrEmpty()
                .stream()
                .filter(CourseCreateRequest.SelectedRecommendationItem::isTour)
                .filter(item -> item.recommendationReason() != null && !item.recommendationReason().isBlank())
                .collect(Collectors.toMap(
                        CourseCreateRequest.SelectedRecommendationItem::tourId,
                        item -> item.recommendationReason().trim(),
                        (first, ignored) -> first
                ));

        log.info(
                "코스 생성 AI 추천 이유 수신: selectedItemCount={}, eventReasonCount={}, tourReasonCount={}, eventIds={}, tourIds={}",
                request.getSelectedRecommendationItemsOrEmpty().size(),
                eventRecommendationReasons.size(),
                tourRecommendationReasons.size(),
                request.eventIds(),
                request.tourIds()
        );

        List<CourseRouteCandidate> selectedPlaces = createRouteCandidates(
                events,
                tours,
                restaurant,
                cafe,
                eventRecommendationReasons,
                tourRecommendationReasons
        );

        List<CourseRouteCandidate> orderedPlaces = findOptimalRoute(
                selectedPlaces,
                request.startLatitude(),
                request.startLongitude()
        );

        return new CourseCreationContext(request, orderedPlaces);
    }

    private Long saveCourse(
            Long memberId,
            CourseCreationContext creationContext,
            String finalCourseRecommendationReason,
            List<String> finalPlaceRecommendationReasons
    ) {
        CourseCreateRequest request = creationContext.request();
        Member member = getMemberOrThrow(memberId);

        Course course = Course.create(
                member,
                request.title(),
                request.description(),
                request.courseType(),
                request.startDate(),
                request.endDate(),
                request.baseArea(),
                request.companionType(),
                request.startLatitude(),
                request.startLongitude(),
                null,
                null,
                finalCourseRecommendationReason
        );

        int order = 1;
        for (int index = 0; index < creationContext.orderedPlaces().size(); index++) {
            addCoursePlace(
                    course,
                    creationContext.orderedPlaces().get(index),
                    order++,
                    finalPlaceRecommendationReasons.get(index)
            );
        }

        courseRepository.save(course);

        return course.getId();
    }

    private List<CourseRouteCandidate> createRouteCandidates(
            List<Event> events,
            List<Tour> tours,
            Place restaurant,
            Place cafe,
            Map<Long, String> eventRecommendationReasons,
            Map<Long, String> tourRecommendationReasons
    ) {
        List<CourseRouteCandidate> candidates = new ArrayList<>();
        int sequence = 0;

        for (Event event : events) {
            Place eventPlace = event.getPlace();
            Double latitude = eventPlace != null && eventPlace.getLatitude() != null
                    ? eventPlace.getLatitude().doubleValue()
                    : event.getLatitude();
            Double longitude = eventPlace != null && eventPlace.getLongitude() != null
                    ? eventPlace.getLongitude().doubleValue()
                    : event.getLongitude();

            candidates.add(CourseRouteCandidate.event(
                    sequence++,
                    event,
                    latitude,
                    longitude,
                    getRecommendationReason(
                            eventRecommendationReasons,
                            event.getId(),
                            "현재 선택한 조건과 행사 유사도를 기반으로 추천되었습니다."
                    )
            ));
        }

        for (Tour tour : tours) {
            candidates.add(CourseRouteCandidate.tour(
                    sequence++,
                    tour,
                    tour.getLatitude(),
                    tour.getLongitude(),
                    getRecommendationReason(
                            tourRecommendationReasons,
                            tour.getId(),
                            "선택한 관광지입니다."
                    )
            ));
        }

        if (restaurant != null) {
            candidates.add(CourseRouteCandidate.place(
                    sequence++,
                    restaurant,
                    restaurant.getLatitude() != null
                            ? restaurant.getLatitude().doubleValue()
                            : null,
                    restaurant.getLongitude() != null
                            ? restaurant.getLongitude().doubleValue()
                            : null,
                    "사용자가 선택한 식당입니다."
            ));
        }

        if (cafe != null) {
            candidates.add(CourseRouteCandidate.place(
                    sequence,
                    cafe,
                    cafe.getLatitude() != null
                            ? cafe.getLatitude().doubleValue()
                            : null,
                    cafe.getLongitude() != null
                            ? cafe.getLongitude().doubleValue()
                            : null,
                    "사용자가 선택한 카페입니다."
            ));
        }

        return candidates;
    }

    private List<CourseRouteCandidate> findOptimalRoute(
            List<CourseRouteCandidate> candidates,
            Double startLatitude,
            Double startLongitude
    ) {
        List<CourseRouteCandidate> routeableCandidates = candidates.stream()
                .filter(CourseRouteCandidate::hasCoordinates)
                .toList();

        List<CourseRouteCandidate> candidatesWithoutCoordinates = candidates.stream()
                .filter(candidate -> !candidate.hasCoordinates())
                .toList();

        if (routeableCandidates.size() < 2) {
            return candidates;
        }

        List<BeamState> beamStates = List.of(
                BeamState.start(startLatitude, startLongitude)
        );

        for (int depth = 0; depth < routeableCandidates.size(); depth++) {
            List<BeamState> expandedStates = new ArrayList<>();

            for (BeamState beamState : beamStates) {
                for (CourseRouteCandidate candidate : routeableCandidates) {
                    if (beamState.visitedSequences().contains(candidate.sequence())) {
                        continue;
                    }

                    double moveDistance = beamState.currentLatitude() == null
                            || beamState.currentLongitude() == null
                            ? 0.0
                            : distance(
                            beamState.currentLatitude(),
                            beamState.currentLongitude(),
                            candidate.latitude(),
                            candidate.longitude()
                    );

                    expandedStates.add(beamState.extend(candidate, moveDistance));
                }
            }

            beamStates = expandedStates.stream()
                    .sorted(Comparator.comparingDouble(BeamState::totalDistance))
                    .limit(DEFAULT_ROUTE_BEAM_WIDTH)
                    .toList();
        }

        if (beamStates.isEmpty()) {
            return candidates;
        }

        List<CourseRouteCandidate> orderedCandidates = new ArrayList<>(
                beamStates.get(0).route()
        );
        orderedCandidates.addAll(candidatesWithoutCoordinates);

        log.info(
                "선택 장소 빔 서치 경로 생성 완료: candidateCount={}, totalDistance={}m",
                candidates.size(),
                beamStates.get(0).totalDistance()
        );

        return orderedCandidates;
    }

    private void addCoursePlace(
            Course course,
            CourseRouteCandidate candidate,
            int visitOrder,
            String recommendationReason
    ) {
        if (candidate.itemType() == CourseItemType.EVENT) {
            Event event = eventRepository.getReferenceById(candidate.event().getId());

            course.addCoursePlace(
                    CoursePlace.create(
                            course,
                            event.getPlace(),
                            event,
                            visitOrder,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            recommendationReason
                    )
            );
            return;
        }

        if (candidate.itemType() == CourseItemType.TOUR) {
            Tour tour = tourRepository.getReferenceById(candidate.tour().getId());

            course.addCoursePlace(
                    CoursePlace.createWithTour(
                            course,
                            tour,
                            visitOrder,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            recommendationReason
                    )
            );
            return;
        }

        course.addCoursePlace(
                CoursePlace.create(
                        course,
                        placeRepository.getReferenceById(candidate.place().getId()),
                        null,
                        visitOrder,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        recommendationReason
                )
        );
    }

    private record CourseCreationContext(
            CourseCreateRequest request,
            List<CourseRouteCandidate> orderedPlaces
    ) {
    }

    private record CourseRouteCandidate(
            int sequence,
            CourseItemType itemType,
            Event event,
            Tour tour,
            Place place,
            Double latitude,
            Double longitude,
            String recommendationReason
    ) {
        private static CourseRouteCandidate event(
                int sequence,
                Event event,
                Double latitude,
                Double longitude,
                String recommendationReason
        ) {
            return new CourseRouteCandidate(
                    sequence,
                    CourseItemType.EVENT,
                    event,
                    null,
                    null,
                    latitude,
                    longitude,
                    recommendationReason
            );
        }

        private static CourseRouteCandidate tour(
                int sequence,
                Tour tour,
                Double latitude,
                Double longitude,
                String recommendationReason
        ) {
            return new CourseRouteCandidate(
                    sequence,
                    CourseItemType.TOUR,
                    null,
                    tour,
                    null,
                    latitude,
                    longitude,
                    recommendationReason
            );
        }

        private static CourseRouteCandidate place(
                int sequence,
                Place place,
                Double latitude,
                Double longitude,
                String recommendationReason
        ) {
            return new CourseRouteCandidate(
                    sequence,
                    CourseItemType.PLACE,
                    null,
                    null,
                    place,
                    latitude,
                    longitude,
                    recommendationReason
            );
        }

        private boolean hasCoordinates() {
            return latitude != null && longitude != null;
        }
    }

    private record BeamState(
            List<CourseRouteCandidate> route,
            Set<Integer> visitedSequences,
            Double currentLatitude,
            Double currentLongitude,
            double totalDistance
    ) {
        private static BeamState start(Double startLatitude, Double startLongitude) {
            return new BeamState(
                    List.of(),
                    Set.of(),
                    startLatitude,
                    startLongitude,
                    0.0
            );
        }

        private BeamState extend(CourseRouteCandidate candidate, double moveDistance) {
            List<CourseRouteCandidate> nextRoute = new ArrayList<>(route);
            nextRoute.add(candidate);

            Set<Integer> nextVisitedSequences = new HashSet<>(visitedSequences);
            nextVisitedSequences.add(candidate.sequence());

            return new BeamState(
                    List.copyOf(nextRoute),
                    Set.copyOf(nextVisitedSequences),
                    candidate.latitude(),
                    candidate.longitude(),
                    totalDistance + moveDistance
            );
        }
    }


    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CoursePreviewResponse previewCourse(Long memberId,
                                               CoursePreviewRequest request) {

        getMemberOrThrow(memberId);
        log.info(
                "추천 코스 미리보기 분기 진입: memberId={}, categories={}",
                memberId,
                request.categories()
        );

        List<Long> recommendedEventIds = distinctIds(request.eventIds());
        List<Long> recommendedTourIds = distinctIds(request.tourIds());

        List<Event> events = eventRepository.findAllById(recommendedEventIds);
        List<Tour> tours = tourRepository.findAllById(recommendedTourIds);

        Category restaurantCategory = getCategoryByName("식당");
        Category cafeCategory = getCategoryByName("카페");

        List<EventNearbyPlaceResponse> nearbyPlaceResponses = new ArrayList<>();

        events.stream()
                .filter(event -> event.getLatitude() != null
                        && event.getLongitude() != null)
                .map(event -> createNearbyPlaceResponse(
                        event,
                        request,
                        restaurantCategory,
                        cafeCategory
                ))
                .forEach(nearbyPlaceResponses::add);

        tours.stream()
                .filter(tour -> tour.getLatitude() != null
                        && tour.getLongitude() != null)
                .map(tour -> createNearbyPlaceResponse(
                        tour,
                        request,
                        restaurantCategory,
                        cafeCategory
                ))
                .forEach(nearbyPlaceResponses::add);

        return new CoursePreviewResponse(
                recommendedEventIds,
                recommendedTourIds,
                nearbyPlaceResponses,
                request.startLatitude(),
                request.startLongitude()
        );
    }

    private EventNearbyPlaceResponse createNearbyPlaceResponse(
            Event event,
            CoursePreviewRequest request,
            Category restaurantCategory,
            Category cafeCategory
    ) {
        return createNearbyPlaceResponse(
                CourseItemType.EVENT,
                event.getId(),
                event.getTitle(),
                event.getLatitude(),
                event.getLongitude(),
                request,
                restaurantCategory,
                cafeCategory
        );
    }

    private EventNearbyPlaceResponse createNearbyPlaceResponse(
            Tour tour,
            CoursePreviewRequest request,
            Category restaurantCategory,
            Category cafeCategory
    ) {
        return createNearbyPlaceResponse(
                CourseItemType.TOUR,
                tour.getId(),
                tour.getTitle(),
                tour.getLatitude(),
                tour.getLongitude(),
                request,
                restaurantCategory,
                cafeCategory
        );
    }

    private EventNearbyPlaceResponse createNearbyPlaceResponse(
            CourseItemType itemType,
            Long itemId,
            String title,
            Double latitude,
            Double longitude,
            CoursePreviewRequest request,
            Category restaurantCategory,
            Category cafeCategory
    ) {
        KakaoPlaceResponse cafeResponse = searchCafeOrEmpty(
                itemType,
                itemId,
                latitude,
                longitude
        );

        RestaurantType restaurantType =
                request.restaurantType() != null
                        ? request.restaurantType()
                        : RestaurantType.KOREAN;

        KakaoPlaceResponse restaurantResponse = searchRestaurantOrEmpty(
                itemType,
                itemId,
                latitude,
                longitude,
                restaurantType
        );

        List<KakaoPlaceDocument> restaurantDocuments = findNearbyDocuments(
                restaurantResponse,
                latitude,
                longitude
        );
        List<KakaoPlaceDocument> cafeDocuments = findNearbyDocuments(
                cafeResponse,
                latitude,
                longitude
        );

        List<PlacePreviewResponse> restaurants = placeService
                .getOrCreatePlaces(restaurantDocuments, restaurantCategory)
                .stream()
                .map(this::toPlacePreviewResponse)
                .toList();

        List<PlacePreviewResponse> cafes = placeService
                .getOrCreatePlaces(cafeDocuments, cafeCategory)
                .stream()
                .map(this::toPlacePreviewResponse)
                .toList();

        return new EventNearbyPlaceResponse(
                itemType.name(),
                itemId,
                title,
                latitude,
                longitude,
                restaurants,
                cafes
        );
    }

    private KakaoPlaceResponse searchCafeOrEmpty(
            CourseItemType itemType,
            Long itemId,
            Double latitude,
            Double longitude
    ) {
        try {
            return kakaoLocalService.searchCafe(latitude, longitude);
        } catch (RestClientException exception) {
            log.warn(
                    "카카오 카페 검색 실패로 빈 목록을 반환합니다. itemType={}, itemId={}, latitude={}, longitude={}",
                    itemType,
                    itemId,
                    latitude,
                    longitude,
                    exception
            );
            return null;
        }
    }

    private KakaoPlaceResponse searchRestaurantOrEmpty(
            CourseItemType itemType,
            Long itemId,
            Double latitude,
            Double longitude,
            RestaurantType restaurantType
    ) {
        try {
            return kakaoLocalService.searchRestaurant(latitude, longitude, restaurantType);
        } catch (RestClientException exception) {
            log.warn(
                    "카카오 식당 검색 실패로 빈 목록을 반환합니다. itemType={}, itemId={}, restaurantType={}, latitude={}, longitude={}",
                    itemType,
                    itemId,
                    restaurantType,
                    latitude,
                    longitude,
                    exception
            );
            return null;
        }
    }

    private List<KakaoPlaceDocument> getDocumentsOrEmpty(KakaoPlaceResponse response) {
        if (response == null || response.documents() == null) {
            return List.of();
        }

        return response.documents();
    }

    private List<KakaoPlaceDocument> findNearbyDocuments(
            KakaoPlaceResponse response,
            Double latitude,
            Double longitude
    ) {
        return getDocumentsOrEmpty(response)
                .stream()
                .filter(document -> document.y() != null && document.x() != null)
                .filter(document -> distance(
                        latitude,
                        longitude,
                        Double.parseDouble(document.y()),
                        Double.parseDouble(document.x())
                ) <= NEARBY_PLACE_RADIUS_METER)
                .limit(5)
                .toList();
    }

    private PlacePreviewResponse toPlacePreviewResponse(Place place) {
        return new PlacePreviewResponse(
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getLatitude() != null ? place.getLatitude().doubleValue() : null,
                place.getLongitude() != null ? place.getLongitude().doubleValue() : null,
                place.getPlaceUrl()
        );
    }

    private Category getCategoryByName(String name) {
        return categoryRepository.findFirstByNameOrderByIdAsc(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }


    @Transactional(readOnly = true)
    public CourseDetailResponse getCourse(Long courseId) {
        log.info("코스 단건 조회 처리 시작: courseId={}", courseId);

        Course course = getCourseOrThrow(courseId);

        List<CoursePlaceResponse> places =
                coursePlaceRepository.findDetailByCourseId(courseId)
                        .stream()
                        .map(this::toCoursePlaceResponse)
                        .toList();

        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCourseType(),
                course.getStartDate(),
                course.getEndDate(),
                course.getBaseArea(),
                course.getCompanionType(),
                course.getStartLatitude(),
                course.getStartLongitude(),
                places,
                course.getRecommendationReason(),
                course.getTotalDistance() != null ? course.getTotalDistance() : 0.0,
                course.getEstimatedTime() != null ? course.getEstimatedTime() : 0,
                course.getAverageRating(),
                course.getReviewCount()
        );
    }

    private CoursePlaceResponse toCoursePlaceResponse(CoursePlace coursePlace) {
        if (coursePlace.getTour() != null) {
            Tour tour = coursePlace.getTour();

            return new CoursePlaceResponse(
                    CourseItemType.TOUR,
                    null,
                    null,
                    tour.getId(),
                    tour.getTitle(),
                    coursePlace.getVisitOrder(),
                    coursePlace.getRecommendationReason(),
                    toBigDecimal(tour.getLatitude()),
                    toBigDecimal(tour.getLongitude()),
                    tour.getAddress()
            );
        }

        if (coursePlace.getEvent() != null) {
            Event event = coursePlace.getEvent();
            Place eventPlace = event.getPlace();

            Long placeId = eventPlace != null ? eventPlace.getId() : null;
            BigDecimal latitude = eventPlace != null
                    ? eventPlace.getLatitude()
                    : toBigDecimal(event.getLatitude());
            BigDecimal longitude = eventPlace != null
                    ? eventPlace.getLongitude()
                    : toBigDecimal(event.getLongitude());
            String address = eventPlace != null
                    ? eventPlace.getAddress()
                    : event.getArea();

            return new CoursePlaceResponse(
                    CourseItemType.EVENT,
                    placeId,
                    event.getId(),
                    null,
                    event.getTitle(),
                    coursePlace.getVisitOrder(),
                    coursePlace.getRecommendationReason(),
                    latitude,
                    longitude,
                    address
            );
        }

        if (coursePlace.getPlace() != null) {
            Place place = coursePlace.getPlace();

            return new CoursePlaceResponse(
                    CourseItemType.PLACE,
                    place.getId(),
                    null,
                    null,
                    place.getName(),
                    coursePlace.getVisitOrder(),
                    coursePlace.getRecommendationReason(),
                    place.getLatitude(),
                    place.getLongitude(),
                    place.getAddress()
            );
        }

        return new CoursePlaceResponse(
                null,
                null,
                null,
                null,
                "알 수 없는 장소",
                coursePlace.getVisitOrder(),
                coursePlace.getRecommendationReason(),
                null,
                null,
                null
        );
    }

    @Transactional(readOnly = true)
    public List<CourseListResponse> getCourses() {
        return courseRepository.findAll()
                .stream()
                .map(course -> new CourseListResponse(
                        course.getId(),
                        course.getTitle(),
                        course.getCourseType(),
                        course.getBaseArea(),
                        course.getStartDate(),
                        course.getAverageRating(),
                        course.getReviewCount()
                ))
                .toList();
    }

    @Transactional
    public void deleteCourse(Long memberId, Long courseId) {
        Course course = getCourseOrThrow(courseId);

        if (!course.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 코스를 삭제할 권한이 없습니다.");
        }

        coursePlaceRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
    }

    @Transactional
    public CourseBookmarkResponse toggleBookmark(Long memberId, Long courseId) {
        log.info("코스 북마크 토글 처리 시작: memberId={}, courseId={}", memberId, courseId);

        Member member = getMemberOrThrow(memberId);
        Course course = getCourseOrThrow(courseId);

        return savedCourseRepository.findByMemberIdAndCourseId(memberId, courseId)
                .map(savedCourse -> {
                    savedCourseRepository.delete(savedCourse);

                    log.info(
                            "코스 북마크 해제 완료: memberId={}, courseId={}",
                            memberId,
                            courseId
                    );

                    return CourseBookmarkResponse.unbookmarked(courseId);
                })
                .orElseGet(() -> saveBookmark(member, course));
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long memberId, Long courseId) {
        getMemberOrThrow(memberId);
        getCourseOrThrow(courseId);

        return savedCourseRepository.existsByMemberIdAndCourseId(memberId, courseId);
    }

    @Transactional(readOnly = true)
    public SavedCoursePageResponse getSavedCourses(Long memberId, int page) {
        getMemberOrThrow(memberId);

        int safePage = Math.max(page, 0);

        Page<SavedCourseResponse> savedCourses =
                savedCourseRepository
                        .findAllByMemberIdOrderByCreatedAtDesc(
                                memberId,
                                PageRequest.of(safePage, SAVED_COURSE_PAGE_SIZE)
                        )
                        .map(SavedCourseResponse::from);

        return SavedCoursePageResponse.from(savedCourses);
    }

    private CourseBookmarkResponse saveBookmark(Member member, Course course) {
        try {
            savedCourseRepository.saveAndFlush(
                    SavedCourse.create(member, course, null)
            );

            log.info(
                    "코스 북마크 등록 완료: memberId={}, courseId={}",
                    member.getId(),
                    course.getId()
            );

            return CourseBookmarkResponse.bookmarked(course.getId());
        } catch (DataIntegrityViolationException e) {
            log.warn(
                    "중복 코스 북마크 요청: memberId={}, courseId={}",
                    member.getId(),
                    course.getId()
            );

            throw new IllegalArgumentException("이미 북마크한 코스입니다.");
        }
    }

    private List<Event> findEventsByIds(List<Long> eventIds) {
        List<Long> uniqueEventIds = distinctIds(eventIds);
        if (uniqueEventIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Event> eventMap = eventRepository.findAllById(uniqueEventIds)
                .stream()
                .collect(Collectors.toMap(Event::getId, event -> event));

        List<Long> missingEventIds = uniqueEventIds.stream()
                .filter(eventId -> !eventMap.containsKey(eventId))
                .toList();

        if (!missingEventIds.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 행사 ID가 포함되어 있습니다: " + missingEventIds);
        }

        return uniqueEventIds.stream()
                .map(eventMap::get)
                .toList();
    }

    private List<Tour> findToursByIds(List<Long> tourIds) {
        List<Long> uniqueTourIds = distinctIds(tourIds);
        if (uniqueTourIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Tour> tourMap = tourRepository.findAllById(uniqueTourIds)
                .stream()
                .collect(Collectors.toMap(Tour::getId, tour -> tour));

        List<Long> missingTourIds = uniqueTourIds.stream()
                .filter(tourId -> !tourMap.containsKey(tourId))
                .toList();

        if (!missingTourIds.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 관광지 ID가 포함되어 있습니다: " + missingTourIds);
        }

        return uniqueTourIds.stream()
                .map(tourMap::get)
                .toList();
    }

    private Place findPlaceById(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                uniqueIds.add(id);
            }
        }

        return List.copyOf(uniqueIds);
    }


    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value);
    }

    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Course getCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double radius = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return radius * c * 1000;
    }

    private String getRecommendationReason(
            Map<Long, String> recommendationReasons,
            Long itemId,
            String defaultReason
    ) {
        String recommendationReason = recommendationReasons.get(itemId);
        return recommendationReason == null || recommendationReason.isBlank()
                ? defaultReason
                : recommendationReason;
    }

    private List<AiRecommendationReasonService.FinalPlaceReasonContext> buildFinalPlaceReasonContexts(
            List<CourseRouteCandidate> orderedPlaces
    ) {
        List<AiRecommendationReasonService.FinalPlaceReasonContext> contexts = new ArrayList<>();

        for (int index = 0; index < orderedPlaces.size(); index++) {
            CourseRouteCandidate candidate = orderedPlaces.get(index);
            String previousPlaceName = index == 0
                    ? null
                    : getCandidateName(orderedPlaces.get(index - 1));
            String nextPlaceName = index == orderedPlaces.size() - 1
                    ? null
                    : getCandidateName(orderedPlaces.get(index + 1));

            contexts.add(new AiRecommendationReasonService.FinalPlaceReasonContext(
                    index + 1,
                    getCandidateName(candidate),
                    getCandidateTypeName(candidate),
                    previousPlaceName,
                    nextPlaceName
            ));
        }

        return contexts;
    }

    private String getCandidateName(CourseRouteCandidate candidate) {
        if (candidate.itemType() == CourseItemType.EVENT) {
            return candidate.event().getTitle();
        }
        if (candidate.itemType() == CourseItemType.TOUR) {
            return candidate.tour().getTitle();
        }
        return candidate.place().getName();
    }

    private String getCandidateTypeName(CourseRouteCandidate candidate) {
        if (candidate.itemType() == CourseItemType.EVENT) {
            return "행사";
        }
        if (candidate.itemType() == CourseItemType.TOUR) {
            return "관광지";
        }
        return "장소";
    }
}
