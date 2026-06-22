package come.back.gotoday.course.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository; // 💡 카테고리 레포 추가
import come.back.gotoday.course.dto.*;
import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.entity.CoursePlace;
import come.back.gotoday.course.repository.CoursePlaceRepository;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.course.type.RestaurantType;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.external.kakao.dto.KakaoPlaceResponse;
import come.back.gotoday.external.kakao.service.KakaoLocalService;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import come.back.gotoday.place.service.PlaceService;
import come.back.gotoday.recommend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;

    private final RecommendationService recommendationService;
    private final EventRepository eventRepository;
    private final KakaoLocalService kakaoLocalService;
    private final PlaceService placeService;


    private double distance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371; // km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(lat1)) *
                                Math.cos(Math.toRadians(lat2)) *
                                Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    //삽입할 인덱스 위치 찾기
    private int findInsertIndex(
            List<Object> route,
            double lat,
            double lng
    ) {

        int bestIndex = route.size();
        double bestScore = Double.MAX_VALUE;

        for (int i = 0; i < route.size() - 1; i++) {

            Double lat1 = getLat(route.get(i));
            Double lng1 = getLng(route.get(i));

            Double lat2 = getLat(route.get(i + 1));
            Double lng2 = getLng(route.get(i + 1));

            if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
                continue;
            }

            double midLat = (lat1 + lat2) / 2;
            double midLng = (lng1 + lng2) / 2;

            double dist = distance(lat, lng, midLat, midLng);

            if (dist < bestScore) {
                bestScore = dist;
                bestIndex = i + 1;
            }
        }

        return bestIndex;
    }

    private Double getLat(Object obj) {

        if (obj instanceof Event e) {
            return e.getLatitude();
        }

        if (obj instanceof Place p) {
            return p.getLatitude() != null
                    ? p.getLatitude().doubleValue()
                    : null;
        }

        return null;
    }

    private Double getLng(Object obj) {

        if (obj instanceof Event e) {
            return e.getLongitude();
        }

        if (obj instanceof Place p) {
            return p.getLongitude() != null
                    ? p.getLongitude().doubleValue()
                    : null;
        }

        return null;
    }
    // 코스 저장 (생성) // 카페, 식당 선택한 정보까지 포함되어 들어온다.
    @Transactional
    public Long createCourse(Long memberId, CourseCreateRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        List<Event> events = request.eventIds().stream()
                .map(id -> eventRepository.findById(id)
                        .orElseThrow())
                .toList();

        if (events.size() < 3) {
            throw new IllegalArgumentException("이벤트는 최소 3개 필요");
        }

        Place restaurant = placeRepository.findById(request.restaurantId())
                .orElseThrow(() -> new IllegalArgumentException("식당 없음"));

        Place cafe = placeRepository.findById(request.cafeId())
                .orElseThrow(() -> new IllegalArgumentException("카페 없음"));

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

        // -------------------------
        // 1. 이벤트 먼저 담기
        // -------------------------
        List<Object> route = new ArrayList<>();

        route.add(events.get(0));
        route.add(events.get(1));
        route.add(events.get(2));

        // -------------------------
        // 2. 식당 삽입
        // -------------------------
        int restaurantIndex = findInsertIndex(
                route,
                restaurant.getLatitude().doubleValue(),
                restaurant.getLongitude().doubleValue()
        );

        route.add(restaurantIndex, restaurant);

        // -------------------------
        // 3. 카페 삽입
        // -------------------------
        int cafeIndex = findInsertIndex(
                route,
                cafe.getLatitude().doubleValue(),
                cafe.getLongitude().doubleValue()
        );

        route.add(cafeIndex, cafe);

        // -------------------------
        // 4. visitOrder 부여하면서 저장
        // -------------------------
        int order = 1;

        for (Object item : route) {

            if (item instanceof Event event) {

                course.addCoursePlace(
                        CoursePlace.create(
                                course,
                                event.getPlace(),
                                event,
                                order++,
                                null, null, null, null, null, null,
                                "행사"
                        )
                );
            }

            else if (item instanceof Place place) {

                String reason =
                        place.getId().equals(restaurant.getId())
                                ? "추천 맛집"
                                : "추천 카페";

                course.addCoursePlace(
                        CoursePlace.create(
                                course,
                                place,
                                null,
                                order++,
                                null, null, null, null, null, null,
                                reason
                        )
                );
            }
        }

        courseRepository.save(course);

        return course.getId();
    }

    //카페, 식당 리스트 해주는 메소드 previewCourse
    @Transactional
    public CoursePreviewResponse previewCourse(Long memberId, CoursePreviewRequest request) {

        String queryText = recommendationService.createQueryText(
                request.baseArea(),
                request.courseType(),
                request.companionType()
        );

        List<Long> recommendedEventIds =
                recommendationService.getRecommendedEventIds(
                        memberId,
                        queryText,
                        request.startDate(),
                        request.endDate(),
                        3
                );

        // 추천된 이벤트 3개 저장함 events
        List<Event> events = eventRepository.findAllById(recommendedEventIds);

        if (events.isEmpty()) {
            throw new IllegalArgumentException("추천 이벤트가 없습니다.");
        }

        // =========================
        // 1. 출발지 기준 (request에 담겨 있는 시작 위경도 받아옴)
        // =========================
        double startLat = request.startLatitude() != null ? request.startLatitude() : 0.0;
        double startLng = request.startLongitude() != null ? request.startLongitude() : 0.0;


        // =========================
        // 2. 중간 지점 계산 (null 안전)
        // =========================

        Event event1 = events.get(0);
        Event event2 = events.get(1);
        Event event3 = events.get(2);

        double midLat = (event2.getLatitude() + event3.getLatitude()) / 2.0;
        double midLng = (event2.getLongitude() + event3.getLongitude()) / 2.0;

        // =========================
        // 3. POI 검색 (event2,event3사이의 식당, 카페 추천)
        // =========================
        KakaoPlaceResponse cafeResponse =
                kakaoLocalService.searchCafe(midLat, midLng);

        RestaurantType restaurantType =
                request.restaurantType() != null
                        ? request.restaurantType()
                        : RestaurantType.KOREAN;

        KakaoPlaceResponse restaurantResponse =
                kakaoLocalService.searchRestaurant(midLat, midLng, restaurantType);

        Category restaurantCategory = categoryRepository.findByName("맛집")
                .orElseThrow(() -> new IllegalArgumentException("맛집 카테고리 없음"));

        Category cafeCategory = categoryRepository.findByName("카페")
                .orElseThrow(() -> new IllegalArgumentException("카페 카테고리 없음"));

        // =========================
        // 4. Place 저장 (Entity 기준)
        // =========================
        List<Place> savedRestaurants = restaurantResponse.documents()
                .stream()
                .map(doc -> placeService.getOrCreatePlace(doc, restaurantCategory))
                .toList();

        List<Place> savedCafes = cafeResponse.documents()
                .stream()
                .map(doc -> placeService.getOrCreatePlace(doc, cafeCategory))
                .toList();

        // =========================
        // 5. DTO 변환
        // =========================
        List<PlacePreviewResponse> restaurantDtos = savedRestaurants.stream()
                .map(p -> new PlacePreviewResponse(
                        p.getId(),
                        p.getName(),
                        p.getAddress(),
                        p.getLatitude() != null ? p.getLatitude().doubleValue() : null,
                        p.getLongitude() != null ? p.getLongitude().doubleValue() : null,
                        p.getPlaceUrl()
                ))
                .toList();

        List<PlacePreviewResponse> cafeDtos = savedCafes.stream()
                .map(p -> new PlacePreviewResponse(
                        p.getId(),
                        p.getName(),
                        p.getAddress(),
                        p.getLatitude() != null ? p.getLatitude().doubleValue() : null,
                        p.getLongitude() != null ? p.getLongitude().doubleValue() : null,
                        p.getPlaceUrl()
                ))
                .toList();

        List<Long> eventIds = List.of(
                event1.getId(),
                event2.getId(),
                event3.getId()
        );

        return new CoursePreviewResponse(
                eventIds,
                restaurantDtos,
                cafeDtos,
                startLat,
                startLng
        );
    }

    // 코스 상세조회
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourse(Long courseId) {
        log.info("코스 단건 조회 처리 시작: courseId={}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        List<CoursePlaceResponse> places =
                coursePlaceRepository.findDetailByCourseId(courseId)
                        .stream()
                        .map(cp -> {
                            Long placeId = null;
                            String placeName = "알 수 없는 장소";

                            // 💡 [수정] 이름 매핑 우선순위 필터링 개조
                            if (cp.getEvent() != null) {
                                // 1. 코스 장소에 '행사(Event)'가 꽂혀있는 경우 -> 행사 정보가 최우선!
                                placeName = cp.getEvent().getTitle(); //행사명 대입한다.
                                if (cp.getEvent().getPlace() != null) {
                                    placeId = cp.getEvent().getPlace().getId();
                                }
                            } else if (cp.getPlace() != null) {
                                // 2. 카카오 API로 받아온 식당/카페처럼 일반 장소('Place')만 꽂혀있는 경우
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
                course.getId(), course.getTitle(), course.getDescription(), course.getCourseType(),
                course.getStartDate(), course.getEndDate(), course.getBaseArea(), course.getCompanionType(),
                course.getStartLatitude(),
                course.getStartLongitude(),
                places,
                course.getTotalDistance() != null ? course.getTotalDistance() : 0.0,
                course.getEstimatedTime() != null ? course.getEstimatedTime() : 0
        );
    }

    // 코스 목록 조회
    @Transactional(readOnly = true)
    public List<CourseListResponse> getCourses() {
        return courseRepository.findAll()
                .stream()
                .map(course -> new CourseListResponse(
                        course.getId(), course.getTitle(), course.getCourseType(),
                        course.getBaseArea(), course.getStartDate()
                ))
                .toList();
    }

    // 코스 삭제
    @Transactional
    public void deleteCourse(Long memberId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if (!course.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 코스를 삭제할 권한이 없습니다.");
        }

        coursePlaceRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
    }
}