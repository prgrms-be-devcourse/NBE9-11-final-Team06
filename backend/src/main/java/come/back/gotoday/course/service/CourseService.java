package come.back.gotoday.course.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.course.dto.CourseBookmarkResponse;
import come.back.gotoday.course.dto.CourseCreateRequest;
import come.back.gotoday.course.dto.CourseDetailResponse;
import come.back.gotoday.course.dto.CourseListResponse;
import come.back.gotoday.course.dto.CoursePlaceResponse;
import come.back.gotoday.course.dto.CoursePreviewRequest;
import come.back.gotoday.course.dto.CoursePreviewResponse;
import come.back.gotoday.course.dto.PlacePreviewResponse;
import come.back.gotoday.course.dto.SavedCoursePageResponse;
import come.back.gotoday.course.dto.SavedCourseResponse;
import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.entity.CoursePlace;
import come.back.gotoday.course.entity.SavedCourse;
import come.back.gotoday.course.repository.CoursePlaceRepository;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.course.repository.SavedCourseRepository;
import come.back.gotoday.course.type.RestaurantType;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.external.kakao.dto.KakaoPlaceResponse;
import come.back.gotoday.external.kakao.service.KakaoLocalService;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import come.back.gotoday.place.service.PlaceService;
import come.back.gotoday.recommend.dto.RecommendationCourseCreateRequest;
import come.back.gotoday.recommend.dto.RecommendationCourseResponse;
import come.back.gotoday.recommend.dto.RecommendedCoursePlaceResponse;
import come.back.gotoday.recommend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private static final int SAVED_COURSE_PAGE_SIZE = 10;
    private static final int DEFAULT_PREVIEW_EVENT_COUNT = 3;
    private static final int DEFAULT_TOUR_PLACE_COUNT = 3;

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final SavedCourseRepository savedCourseRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;

    private final RecommendationService recommendationService;
    private final EventRepository eventRepository;
    private final KakaoLocalService kakaoLocalService;
    private final PlaceService placeService;

    @Transactional
    public Long createCourse(Long memberId, CourseCreateRequest request) {
        Member member = getMemberOrThrow(memberId);

        List<Event> events = List.of();

        if (request.eventIds() != null && !request.eventIds().isEmpty()) {
            Map<Long, Event> eventMap = eventRepository.findAllById(request.eventIds())
                    .stream()
                    .collect(Collectors.toMap(Event::getId, event -> event));

            events = request.eventIds()
                    .stream()
                    .map(eventMap::get)
                    .filter(event -> event != null)
                    .toList();
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

        if (request.restaurantId() != null) {
            Place restaurant = placeRepository.findById(request.restaurantId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));

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
                            "추천 맛집"
                    )
            );
        }

        if (request.cafeId() != null) {
            Place cafe = placeRepository.findById(request.cafeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));

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
                            "추천 카페"
                    )
            );
        }

        courseRepository.save(course);

        return course.getId();
    }

    @Transactional
    public RecommendationCourseResponse createRecommendedCourse(
            Long memberId,
            RecommendationCourseCreateRequest request
    ) {
        Member member = getMemberOrThrow(memberId);

        if (hasTourCategory(request)) {
            log.info(
                    "관광지 기반 추천 코스 생성 분기 진입: memberId={}, categories={}",
                    memberId,
                    request.categories()
            );

            return createTourRecommendedCourse(member, request);
        }

        log.info(
                "행사 기반 추천 코스 생성 분기 진입: memberId={}, categories={}",
                memberId,
                request.categories()
        );

        RecommendationService.RecommendedCourseDraft draft =
                recommendationService.recommendCourse(memberId, request);

        Course course = Course.create(
                member,
                draft.title(),
                "추천 알고리즘으로 생성된 코스입니다.",
                "RECOMMENDATION",
                draft.startDate(),
                draft.endDate(),
                draft.baseArea(),
                draft.companionType(),
                draft.latitude(),
                draft.longitude(),
                null,
                null,
                "현재 선택한 조건과 행사 유사도를 기반으로 추천되었습니다."
        );

        Map<Long, Event> eventMap = eventRepository.findAllById(
                        draft.events().stream()
                                .map(RecommendationService.RecommendedEvent::eventId)
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(Event::getId, event -> event));

        List<RecommendedCoursePlaceResponse> places = new ArrayList<>();

        for (RecommendationService.RecommendedEvent recommendedEvent : draft.events()) {
            Event event = eventMap.get(recommendedEvent.eventId());

            if (event == null) {
                continue;
            }

            Place place = getOrCreatePlaceFromEvent(event);

            course.addCoursePlace(
                    CoursePlace.create(
                            course,
                            place,
                            event,
                            recommendedEvent.visitOrder(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            recommendedEvent.reason()
                    )
            );

            places.add(
                    new RecommendedCoursePlaceResponse(
                            event.getId(),
                            place.getId(),
                            event.getTitle(),
                            event.getCategory().getName(),
                            event.getArea(),
                            event.getStartDate(),
                            event.getEndDate(),
                            place.getLatitude(),
                            place.getLongitude(),
                            recommendedEvent.visitOrder(),
                            recommendedEvent.reason()
                    )
            );
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

    private RecommendationCourseResponse createTourRecommendedCourse(
            Member member,
            RecommendationCourseCreateRequest request
    ) {
        List<Place> tourPlaces = placeRepository.findActivePlacesBySourceAndArea(
                        Place.TOUR_API_SOURCE,
                        normalizeArea(request.area())
                )
                .stream()
                .limit(request.getTopKOrDefault())
                .toList();

        if (tourPlaces.isEmpty()) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }

        Course course = Course.create(
                member,
                request.getTitleOrDefault(),
                "관광공사 관광지 데이터를 기반으로 생성된 코스입니다.",
                "RECOMMENDATION",
                request.startDate(),
                request.endDate(),
                request.area(),
                request.companionType(),
                request.latitude(),
                request.longitude(),
                null,
                null,
                "선택한 관광 카테고리와 지역의 관광지 데이터를 기반으로 추천되었습니다."
        );

        List<RecommendedCoursePlaceResponse> places = new ArrayList<>();

        int visitOrder = 1;

        for (Place place : tourPlaces) {
            String reason = "선택한 지역의 관광지 데이터를 기반으로 추천되었습니다.";

            course.addCoursePlace(
                    CoursePlace.create(
                            course,
                            place,
                            null,
                            visitOrder,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            reason
                    )
            );

            places.add(
                    new RecommendedCoursePlaceResponse(
                            null,
                            place.getId(),
                            place.getName(),
                            place.getCategory().getName(),
                            place.getAddress(),
                            null,
                            null,
                            place.getLatitude(),
                            place.getLongitude(),
                            visitOrder,
                            reason
                    )
            );

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

    @Transactional
    public CoursePreviewResponse previewCourse(Long memberId, CoursePreviewRequest request) {
        getMemberOrThrow(memberId);

        if (hasTourCategory(request.categories())) {
            log.info(
                    "관광지 기반 코스 미리보기 분기 진입: memberId={}, categories={}",
                    memberId,
                    request.categories()
            );

            return previewTourCourse(request);
        }

        log.info(
                "행사 기반 코스 미리보기 분기 진입: memberId={}, categories={}",
                memberId,
                request.categories()
        );

        RecommendationService.RecommendedCourseDraft draft =
                recommendationService.recommendCourse(
                        memberId,
                        toRecommendationCourseCreateRequest(request)
                );

        List<Long> recommendedEventIds = draft.events()
                .stream()
                .map(RecommendationService.RecommendedEvent::eventId)
                .toList();

        List<Event> events = eventRepository.findAllById(recommendedEventIds);

        if (events.isEmpty()) {
            throw new IllegalArgumentException("추천 가능한 행사가 없습니다.");
        }

        Event centerEvent = events.size() > 1 ? events.get(1) : events.get(0);

        double centerLat = centerEvent.getLatitude();
        double centerLng = centerEvent.getLongitude();

        return createCoursePreviewResponse(
                recommendedEventIds,
                centerLat,
                centerLng,
                request
        );
    }

    private RecommendationCourseCreateRequest toRecommendationCourseCreateRequest(
            CoursePreviewRequest request
    ) {
        return new RecommendationCourseCreateRequest(
                null,
                request.startDate(),
                request.endDate(),
                DEFAULT_PREVIEW_EVENT_COUNT,
                request.baseArea(),
                request.categories(),
                request.companionType(),
                null,
                request.startLatitude(),
                request.startLongitude()
        );
    }

    private CoursePreviewResponse previewTourCourse(CoursePreviewRequest request) {
        List<Place> tourPlaces = placeRepository.findActivePlacesBySourceAndArea(
                        Place.TOUR_API_SOURCE,
                        normalizeArea(request.baseArea())
                )
                .stream()
                .limit(DEFAULT_TOUR_PLACE_COUNT)
                .toList();

        if (tourPlaces.isEmpty()) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }

        Place centerPlace = tourPlaces.stream()
                .filter(place -> place.getLatitude() != null && place.getLongitude() != null)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));

        return createCoursePreviewResponse(
                List.of(),
                centerPlace.getLatitude().doubleValue(),
                centerPlace.getLongitude().doubleValue(),
                request
        );
    }

    private CoursePreviewResponse createCoursePreviewResponse(
            List<Long> recommendedEventIds,
            double centerLat,
            double centerLng,
            CoursePreviewRequest request
    ) {
        KakaoPlaceResponse cafeResponse =
                kakaoLocalService.searchCafe(centerLat, centerLng);

        RestaurantType restaurantType =
                request.restaurantType() != null
                        ? request.restaurantType()
                        : RestaurantType.KOREAN;

        KakaoPlaceResponse restaurantResponse =
                kakaoLocalService.searchRestaurant(centerLat, centerLng, restaurantType);

        Category restaurantCategory = getCategoryByName("맛집");
        Category cafeCategory = getCategoryByName("카페");

        List<Place> savedRestaurants = restaurantResponse.documents()
                .stream()
                .filter(doc -> doc.y() != null && doc.x() != null)
                .filter(doc -> distance(
                        centerLat,
                        centerLng,
                        Double.parseDouble(doc.y()),
                        Double.parseDouble(doc.x())
                ) <= 500)
                .map(doc -> placeService.getOrCreatePlace(doc, restaurantCategory))
                .toList();

        List<Place> savedCafes = cafeResponse.documents()
                .stream()
                .filter(doc -> doc.y() != null && doc.x() != null)
                .filter(doc -> distance(
                        centerLat,
                        centerLng,
                        Double.parseDouble(doc.y()),
                        Double.parseDouble(doc.x())
                ) <= 500)
                .map(doc -> placeService.getOrCreatePlace(doc, cafeCategory))
                .toList();

        return new CoursePreviewResponse(
                recommendedEventIds,
                savedRestaurants.stream()
                        .map(p -> new PlacePreviewResponse(
                                p.getId(),
                                p.getName(),
                                p.getAddress(),
                                p.getLatitude() != null ? p.getLatitude().doubleValue() : null,
                                p.getLongitude() != null ? p.getLongitude().doubleValue() : null,
                                p.getPlaceUrl()
                        ))
                        .toList(),
                savedCafes.stream()
                        .map(p -> new PlacePreviewResponse(
                                p.getId(),
                                p.getName(),
                                p.getAddress(),
                                p.getLatitude() != null ? p.getLatitude().doubleValue() : null,
                                p.getLongitude() != null ? p.getLongitude().doubleValue() : null,
                                p.getPlaceUrl()
                        ))
                        .toList(),
                request.startLatitude(),
                request.startLongitude()
        );
    }

    private Category getCategoryByName(String name) {
        return categoryRepository.findFirstByNameOrderByIdAsc(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private boolean hasTourCategory(RecommendationCourseCreateRequest request) {
        return hasTourCategory(request.categories());
    }

    private boolean hasTourCategory(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return false;
        }

        return categories.stream()
                .filter(category -> category != null && !category.isBlank())
                .map(category -> category.trim().toUpperCase())
                .anyMatch(category ->
                        "TOUR".equals(category)
                                || "TOUR_PLACE".equals(category)
                                || "TOURISM".equals(category)
                                || "TRAVEL".equals(category)
                                || category.contains("관광")
                                || category.contains("여행")
                );
    }

    private String normalizeArea(String area) {
        if (area == null || area.isBlank()) {
            return null;
        }

        String trimmedArea = area.trim();

        if ("서울특별시".equals(trimmedArea)) {
            return "서울";
        }

        if (trimmedArea.startsWith("서울특별시 ")) {
            return trimmedArea.replaceFirst("^서울특별시\\s*", "").trim();
        }

        return trimmedArea;
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
                event.getLatitude() != null ? BigDecimal.valueOf(event.getLatitude()) : null,
                event.getLongitude() != null ? BigDecimal.valueOf(event.getLongitude()) : null,
                null,
                event.getHomepageUrl(),
                event.getDescription(),
                event.getSource(),
                event.getExternalId(),
                true
        );

        Place savedPlace = placeRepository.save(place);
        event.updatePlace(savedPlace);

        return savedPlace;
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourse(Long courseId) {
        log.info("코스 단건 조회 처리 시작: courseId={}", courseId);

        Course course = getCourseOrThrow(courseId);

        List<CoursePlaceResponse> places =
                coursePlaceRepository.findDetailByCourseId(courseId)
                        .stream()
                        .map(cp -> {
                            Long placeId = null;
                            String placeName = "알 수 없는 장소";

                            if (cp.getEvent() != null) {
                                placeName = cp.getEvent().getTitle();

                                if (cp.getEvent().getPlace() != null) {
                                    placeId = cp.getEvent().getPlace().getId();
                                }
                            } else if (cp.getPlace() != null) {
                                placeId = cp.getPlace().getId();
                                placeName = cp.getPlace().getName();
                            }

                            Place placeEntity = cp.getPlace();
                            Event eventEntity = cp.getEvent();

                            BigDecimal lat = null;
                            BigDecimal lng = null;
                            String address = null;

                            if (placeEntity != null) {
                                lat = placeEntity.getLatitude();
                                lng = placeEntity.getLongitude();
                                address = placeEntity.getAddress();
                            } else if (eventEntity != null && eventEntity.getPlace() != null) {
                                lat = eventEntity.getPlace().getLatitude();
                                lng = eventEntity.getPlace().getLongitude();
                                address = eventEntity.getPlace().getAddress();
                            }

                            return new CoursePlaceResponse(
                                    placeId,
                                    placeName,
                                    cp.getVisitOrder(),
                                    cp.getRecommendationReason(),
                                    lat,
                                    lng,
                                    address
                            );
                        })
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

    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Course getCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000;
    }
}