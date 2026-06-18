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
import come.back.gotoday.external.kakao.dto.KakaoPlaceDocument;
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

    // 코스 저장 (생성)
    @Transactional
    public Long createCourse(Long memberId, CourseCreateRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        List<Event> events = request.eventIds() != null ? eventRepository.findAllById(request.eventIds()) : List.of();

        Course course = Course.create(
                member,
                request.title(),
                request.description(),
                request.courseType(),
                request.startDate(),
                request.endDate(),
                request.baseArea(),
                request.companionType(),
                null,
                null,
                "사용자 선택 코스"
        );

        int order = 1;

        // 1. 이벤트
        for (Event event : events) {
            course.addCoursePlace(
                    CoursePlace.create(
                            course,
                            event.getPlace(),
                            event,
                            order++,
                            null, null, null, null, null, null,
                            "이벤트 기반 추천"
                    )
            );
        }

        // 2. 식당 (ID 기반)
        if (request.restaurantId() != null) {
            Place restaurant = placeRepository.findById(request.restaurantId())
                    .orElseThrow(() -> new IllegalArgumentException("식당 없음"));

            course.addCoursePlace(
                    CoursePlace.create(
                            course,
                            restaurant,
                            null,
                            order++,
                            null, null, null, null, null, null,
                            "추천 맛집"
                    )
            );
        }

        // 3. 카페 (ID 기반)
        if (request.cafeId() != null) {
            Place cafe = placeRepository.findById(request.cafeId())
                    .orElseThrow(() -> new IllegalArgumentException("카페 없음"));

            course.addCoursePlace(
                    CoursePlace.create(
                            course,
                            cafe,
                            null,
                            order++,
                            null, null, null, null, null, null,
                            "추천 카페"
                    )
            );
        }

        courseRepository.save(course);

        return course.getId();
    }


    @Transactional
    public CoursePreviewResponse previewCourse(Long memberId, CourseCreateRequest request) {

        String queryText = recommendationService.createQueryText(
                request.baseArea(),
                request.courseType(),
                request.companionType()
        );

        List<Long> recommendedEventIds =
                recommendationService.getRecommendedEventIds(
                        memberId, queryText,
                        request.startDate(),
                        request.endDate(),
                        3
                );

        List<Event> events = eventRepository.findAllById(recommendedEventIds);

        double centerLat = events.stream()
                .filter(e -> e.getLatitude() != null)
                .mapToDouble(Event::getLatitude)
                .average()
                .orElseThrow(() -> new IllegalArgumentException("유효한 위도 정보가 없습니다."));
        double centerLng = events.stream()
                .filter(e -> e.getLongitude() != null)
                .mapToDouble(Event::getLongitude)
                .average()
                .orElseThrow(() -> new IllegalArgumentException("유효한 경도 정보가 없습니다."));

        KakaoPlaceResponse cafeResponse =
                kakaoLocalService.searchCafe(centerLat, centerLng);

        //기본값 한식 설정
        KakaoPlaceResponse restaurantResponse =
                kakaoLocalService.searchRestaurant(centerLat, centerLng, RestaurantType.KOREAN);

        Category restaurantCategory = categoryRepository.findByName("맛집")
                .orElseThrow(() -> new IllegalArgumentException("맛집 카테고리 없음"));

        Category cafeCategory = categoryRepository.findByName("카페")
                .orElseThrow(() -> new IllegalArgumentException("카페 카테고리 없음"));

        // 🔥 핵심: 여기서 DB 저장 (UPsert)
        List<Place> savedRestaurants = restaurantResponse.documents()
                .stream()
                .map(doc -> placeService.getOrCreatePlace(doc, restaurantCategory))
                .toList();

        List<Place> savedCafes = cafeResponse.documents()
                .stream()
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
                                p.getPlaceUrl()   // ← 이거 없으면 아래 참고
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
                        .toList()
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
                                placeName = cp.getEvent().getTitle(); // 🎯 행사명을 바로 대입!
                                if (cp.getEvent().getPlace() != null) {
                                    placeId = cp.getEvent().getPlace().getId();
                                }
                            } else if (cp.getPlace() != null) {
                                // 2. 카카오 API로 받아온 식당/카페처럼 일반 장소('Place')만 꽂혀있는 경우
                                placeId = cp.getPlace().getId();
                                placeName = cp.getPlace().getName();
                            }

                            return new CoursePlaceResponse(
                                    placeId,
                                    placeName,
                                    cp.getVisitOrder(),
                                    cp.getRecommendationReason()
                            );
                        })
                        .toList();

        return new CourseDetailResponse(
                course.getId(), course.getTitle(), course.getDescription(), course.getCourseType(),
                course.getStartDate(), course.getEndDate(), course.getBaseArea(), course.getCompanionType(),
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