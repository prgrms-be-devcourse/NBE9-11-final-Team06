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

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private static final int SAVED_COURSE_PAGE_SIZE = 10;
    private static final int DEFAULT_PREVIEW_EVENT_COUNT = 3;
    private static final double NEARBY_PLACE_RADIUS_METER = 500.0;

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

    @Transactional
    public Long createCourse(Long memberId, CourseCreateRequest request) {
        Member member = getMemberOrThrow(memberId);

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
                "사용자 선택 코스"
        );

        int order = 1;

        for (Event event : events) {
            course.addCoursePlace(
                    CoursePlace.create(
                            course,
                            event.getPlace(),
                            event,
                            order++,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "현재 선택한 조건과 행사 유사도를 기반으로 추천되었습니다."
                    )
            );
        }

        for (Tour tour : tours) {
            course.addCoursePlace(
                    CoursePlace.createWithTour(
                            course,
                            tour,
                            order++,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "선택한 관광지입니다."
                    )
            );
        }

        if (restaurant != null) {
            course.addCoursePlace(
                    CoursePlace.create(
                            course,
                            restaurant,
                            null,
                            order++,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "사용자가 선택한 식당입니다."
                    )
            );
        }

        if (cafe != null) {
            course.addCoursePlace(
                    CoursePlace.create(
                            course,
                            cafe,
                            null,
                            order++,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "사용자가 선택한 카페입니다."
                    )
            );
        }

        courseRepository.save(course);

        return course.getId();
    }


    @Transactional
    public CoursePreviewResponse previewCourse(Long memberId,
                                               CoursePreviewRequest request) {

        getMemberOrThrow(memberId);
        log.info(
                "추천 코스 미리보기 분기 진입: memberId={}, categories={}",
                memberId,
                request.categories()
        );


        List<Long> recommendedEventIds = request.eventIds();

        List<Event> events = eventRepository.findAllById(recommendedEventIds);

        List<EventNearbyPlaceResponse> nearbyPlaceResponses =
                events.stream()
                        .filter(event -> event.getLatitude() != null
                                && event.getLongitude() != null)
                        .map(event -> createNearbyPlaceResponse(event, request))
                        .toList();

        return new CoursePreviewResponse(
                recommendedEventIds,
                nearbyPlaceResponses,
                request.startLatitude(),
                request.startLongitude()
        );
    }

    private EventNearbyPlaceResponse createNearbyPlaceResponse(
            Event event,
            CoursePreviewRequest request
    ) {
        double latitude = event.getLatitude();
        double longitude = event.getLongitude();

        KakaoPlaceResponse cafeResponse =
                kakaoLocalService.searchCafe(latitude, longitude);

        RestaurantType restaurantType =
                request.restaurantType() != null
                        ? request.restaurantType()
                        : RestaurantType.KOREAN;

        KakaoPlaceResponse restaurantResponse =
                kakaoLocalService.searchRestaurant(latitude, longitude, restaurantType);

        Category restaurantCategory = getCategoryByName("맛집");
        Category cafeCategory = getCategoryByName("카페");

        List<PlacePreviewResponse> restaurants =
                getDocumentsOrEmpty(restaurantResponse)
                        .stream()
                        .filter(doc -> doc.y() != null && doc.x() != null)
                        .filter(doc -> distance(
                                latitude,
                                longitude,
                                Double.parseDouble(doc.y()),
                                Double.parseDouble(doc.x())
                        ) <= NEARBY_PLACE_RADIUS_METER)
                        .limit(5)
                        .map(doc -> placeService.getOrCreatePlace(doc, restaurantCategory))
                        .map(this::toPlacePreviewResponse)
                        .toList();

        List<PlacePreviewResponse> cafes =
                getDocumentsOrEmpty(cafeResponse)
                        .stream()
                        .filter(doc -> doc.y() != null && doc.x() != null)
                        .filter(doc -> distance(
                                latitude,
                                longitude,
                                Double.parseDouble(doc.y()),
                                Double.parseDouble(doc.x())
                        ) <= NEARBY_PLACE_RADIUS_METER)
                        .limit(5)
                        .map(doc -> placeService.getOrCreatePlace(doc, cafeCategory))
                        .map(this::toPlacePreviewResponse)
                        .toList();

        return new EventNearbyPlaceResponse(
                event.getId(),
                event.getTitle(),
                restaurants,
                cafes
        );
    }

    private List<KakaoPlaceDocument> getDocumentsOrEmpty(KakaoPlaceResponse response) {
        if (response == null || response.documents() == null) {
            return List.of();
        }

        return response.documents();
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
                course.getTotalDistance() != null ? course.getTotalDistance() : 0.0,
                course.getEstimatedTime() != null ? course.getEstimatedTime() : 0
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
                        course.getStartDate()
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
}