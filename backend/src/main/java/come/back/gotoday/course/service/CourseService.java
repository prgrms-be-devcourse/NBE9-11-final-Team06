package come.back.gotoday.course.service;

import come.back.gotoday.course.dto.*;
import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.entity.CoursePlace;
import come.back.gotoday.course.repository.CoursePlaceRepository;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.external.kakao.dto.KakaoPlaceDocument;
import come.back.gotoday.external.kakao.dto.KakaoPlaceResponse;
import come.back.gotoday.external.kakao.service.KakaoLocalService;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import come.back.gotoday.preference.repository.UserPreferenceCategoryRepository;
import come.back.gotoday.preference.repository.UserPreferenceRepository;
import come.back.gotoday.recommend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final RecommendationService recommendationService;
    private final EventRepository eventRepository;
    private final KakaoLocalService kakaoLocalService;

    //코스저장
    @Transactional
    public Long createCourse(Long memberId, CourseCreateRequest request) {
        log.info("코스 생성 처리 시작: memberId={}, title={}, placeCount={}", memberId, request.title(), request.placeIds().size());

        // 해당 방법으로 추천된 행사 아이디를 가져올 수 있습니다. getRecommendedEventIds 끝에 숫자는 추천하는 행사의 개수입니다.
        String queryText = recommendationService.createQueryText(request.baseArea(), request.courseType(),  request.companionType());

        log.info("queryText={}", queryText);
        //  AI 추천 엔진 호출 (획득한 queryText 전달)
        List<Long> recommendedEventIds = recommendationService.getRecommendedEventIds(
                memberId, queryText, request.startDate(), request.endDate(), 3
       );
        log.info("출력된 이벤트 아이디:  {}", recommendedEventIds);

        List<Event> events = eventRepository.findAllById(recommendedEventIds);
        if (events.isEmpty()) {
            throw new IllegalArgumentException("추천된 행사가 없습니다.");
        }

        double centerLat = events.stream()
                .mapToDouble(Event::getLatitude)
                .average()
                .orElseThrow();

        double centerLng = events.stream()
                .mapToDouble(Event::getLongitude)
                .average()
                .orElseThrow();

        log.info("행사 중심 좌표 계산 완료 lat={}, lng={}", centerLat, centerLng);
//
//        KakaoPlaceResponse cafeResponse =
//                kakaoLocalService.searchCafe(
//                        event.getLatitude(),
//                        event.getLongitude()
//                );
//
//        KakaoPlaceDocument cafe =
//                cafeResponse.documents().stream()
//                        .findFirst()
//                        .orElse(null);
//
//        KakaoPlaceResponse restaurantResponse =
//                kakaoLocalService.searchRestaurant(
//                        event.getLatitude(),
//                        event.getLongitude()
//                );
//
//        KakaoPlaceDocument restaurant =
//                restaurantResponse.documents().stream()
//                        .findFirst()
//                        .orElse(null);
//


        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("코스 생성 실패: 존재하지 않는 회원입니다. memberId={}", memberId);
                    return new IllegalArgumentException("회원이 존재하지 않습니다.");
                });

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
                null
        );

        List<Place> places = placeRepository.findAllById(request.placeIds());
        java.util.Map<Long, Place> placeMap = places.stream()
                .collect(java.util.stream.Collectors.toMap(Place::getId, java.util.function.Function.identity()));

        log.info("코스 생성 장소 조회 완료: requestedPlaceCount={}, foundPlaceCount={}", request.placeIds().size(), places.size());

        int order = 1;
        for (Long placeId : request.placeIds()) {
            Place place = placeMap.get(placeId);
            if (place == null) {
                log.warn("코스 생성 실패: 존재하지 않는 장소입니다. memberId={}, placeId={}", memberId, placeId);
                throw new IllegalArgumentException("장소가 존재하지 않습니다.");
            }

            CoursePlace coursePlace = CoursePlace.create(
                    course,
                    place,
                    null,
                    order++,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            course.addCoursePlace(coursePlace);
        }

        courseRepository.save(course);

        log.info("코스 생성 처리 완료: memberId={}, courseId={}, placeCount={}", memberId, course.getId(), request.placeIds().size());

        return course.getId();
    }

    //코스 상세조회 - 조회만 하기 때문에 readOnly
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourse(Long courseId) {
        log.info("코스 단건 조회 처리 시작: courseId={}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    log.warn("코스 단건 조회 실패: 존재하지 않는 코스입니다. courseId={}", courseId);
                    return new IllegalArgumentException("존재하지 않는 코스입니다.");
                });

        List<CoursePlaceResponse> places =
                coursePlaceRepository.findDetailByCourseId(courseId)
                        .stream()
                        .map(cp -> new CoursePlaceResponse(
                                cp.getPlace().getId(),
                                cp.getPlace().getName(),
                                cp.getVisitOrder(),
                                cp.getRecommendationReason()
                        ))
                        .toList();

        log.info("코스 장소 상세 조회 완료: courseId={}, placeCount={}", courseId, places.size());

        CourseDetailResponse response = new CourseDetailResponse(
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

        log.info("코스 단건 조회 처리 완료: courseId={}", courseId);
        return response;
    }

    // 코스 목록 조회
    @Transactional(readOnly = true)
    public List<CourseListResponse> getCourses() {
        log.info("코스 목록 조회 처리 시작");

        List<CourseListResponse> courses = courseRepository.findAll()
                .stream()
                .map(course -> new CourseListResponse(
                        course.getId(),
                        course.getTitle(),
                        course.getCourseType(),
                        course.getBaseArea(),
                        course.getStartDate()
                ))
                .toList();

        log.info("코스 목록 조회 처리 완료: resultCount={}", courses.size());
        return courses;
    }

    //코스 삭제
    @Transactional
    public void deleteCourse(
            Long memberId,
            Long courseId
    ) {
        log.info("코스 삭제 처리 시작: memberId={}, courseId={}", memberId, courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    log.warn("코스 삭제 실패: 존재하지 않는 코스입니다. memberId={}, courseId={}", memberId, courseId);
                    return new IllegalArgumentException("존재하지 않는 코스입니다.");
                });

        if (!course.getMember().getId().equals(memberId)) {
            log.warn("코스 삭제 실패: 권한이 없습니다. requestMemberId={}, courseOwnerId={}, courseId={}", memberId, course.getMember().getId(), courseId);
            throw new IllegalArgumentException("해당 코스를 삭제할 권한이 없습니다.");
        }

        coursePlaceRepository.deleteByCourseId(courseId);
        log.info("코스 장소 연결 삭제 완료: courseId={}", courseId);

        courseRepository.delete(course);
        log.info("코스 삭제 처리 완료: memberId={}, courseId={}", memberId, courseId);
    }

}