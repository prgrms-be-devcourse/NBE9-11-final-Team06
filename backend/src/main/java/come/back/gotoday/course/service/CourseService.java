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
import org.springframework.dao.DataIntegrityViolationException;
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
    private final SavedCourseRepository savedCourseRepository;
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

        Member member = getMemberOrThrow(memberId);

        List<Event> events = request.eventIds() != null
                ? eventRepository.findAllById(request.eventIds())
                : List.of();

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
                    .orElseThrow(() -> new IllegalArgumentException("식당 없음"));

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
                    .orElseThrow(() -> new IllegalArgumentException("카페 없음"));

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

    // 행사 데이터를 바탕으로, 주변 식당과 카페 리스트
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

        RestaurantType restaurantType =
                request.restaurantType() != null
                        ? request.restaurantType()
                        : RestaurantType.KOREAN;

        KakaoPlaceResponse restaurantResponse =
                kakaoLocalService.searchRestaurant(centerLat, centerLng, restaurantType);

        Category restaurantCategory = categoryRepository.findByName("맛집")
                .orElseThrow(() -> new IllegalArgumentException("맛집 카테고리 없음"));

        Category cafeCategory = categoryRepository.findByName("카페")
                .orElseThrow(() -> new IllegalArgumentException("카페 카테고리 없음"));

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
                        .toList()
        );
    }

    // 코스 상세조회
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
                        course.getId(),
                        course.getTitle(),
                        course.getCourseType(),
                        course.getBaseArea(),
                        course.getStartDate()
                ))
                .toList();
    }

    // 코스 삭제
    @Transactional
    public void deleteCourse(Long memberId, Long courseId) {
        Course course = getCourseOrThrow(courseId);

        if (!course.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 코스를 삭제할 권한이 없습니다.");
        }

        coursePlaceRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
    }

    // 코스 북마크 등록/해제
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

    // 코스 북마크 여부 조회
    @Transactional(readOnly = true)
    public boolean isBookmarked(Long memberId, Long courseId) {
        getMemberOrThrow(memberId);
        getCourseOrThrow(courseId);

        return savedCourseRepository.existsByMemberIdAndCourseId(memberId, courseId);
    }

    // 내가 북마크한 코스 목록 조회
    @Transactional(readOnly = true)
    public List<SavedCourseResponse> getSavedCourses(Long memberId) {
        getMemberOrThrow(memberId);

        return savedCourseRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(SavedCourseResponse::from)
                .toList();
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
                    "코스 북마크 중복 등록 시도: memberId={}, courseId={}",
                    member.getId(),
                    course.getId()
            );

            throw new IllegalArgumentException("이미 북마크한 코스입니다.");
        }
    }

    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
    }

    private Course getCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));
    }
}