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

    private final RecommendationService recommendationService;
    private final EventRepository eventRepository;
    private final KakaoLocalService kakaoLocalService;

    // 코스 저장 (생성)
    @Transactional
    public Long createCourse(Long memberId, CourseCreateRequest request) {
        log.info("코스 생성 처리 시작: memberId={}, title={}", memberId, request.title());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        String queryText = recommendationService.createQueryText(request.baseArea(), request.courseType(), request.companionType());
        List<Long> recommendedEventIds = recommendationService.getRecommendedEventIds(
                memberId, queryText, request.startDate(), request.endDate(), 3
        );

        List<Event> events = eventRepository.findAllById(recommendedEventIds);
        if (events.isEmpty()) {
            throw new IllegalArgumentException("추천된 행사가 없습니다.");
        }

        double centerLat = events.stream().mapToDouble(Event::getLatitude).average().orElseThrow();
        double centerLng = events.stream().mapToDouble(Event::getLongitude).average().orElseThrow();

        KakaoPlaceResponse cafeResponse = kakaoLocalService.searchCafe(centerLat, centerLng);
        KakaoPlaceDocument cafe = cafeResponse.documents().stream().findFirst().orElse(null);

        KakaoPlaceResponse restaurantResponse = kakaoLocalService.searchRestaurant(centerLat, centerLng);
        KakaoPlaceDocument restaurant = restaurantResponse.documents().stream().findFirst().orElse(null);

        Course course = Course.create(
                member, request.title(), request.description(), request.courseType(),
                request.startDate(), request.endDate(), request.baseArea(), request.companionType(),
                null, null, "AI 추천 행사 및 주변 장소 기반 코스"
        );

        // 취향 저격용 추천 사유 베이스 문구 생성
        String userPreferenceText = String.format("%s에서 %s와(과) 함께 즐기는 %s 코스",
                request.baseArea(), request.companionType(), request.courseType());

        int order = 1;

        // ① AI 추천 행사들 추가 (11개 파라미터 자리 완벽 일치)
        for (Event event : events) {
            CoursePlace coursePlace = CoursePlace.create(
                    course,
                    event.getPlace(),
                    event,
                    order++,
                    null, null, null, null, null, null, // 💡 형이 쓰던 오리지널 null 6개 복구
                    String.format("%s||[%s] 맞춤 추천 행사입니다.", event.getTitle(), userPreferenceText) // 맨 마지막 파라미터가 사유 자리!
            );
            course.addCoursePlace(coursePlace);
        }

        // ② 주변 추천 식당 추가
        if (restaurant != null) {
            CoursePlace restaurantCoursePlace = CoursePlace.create(
                    course,
                    null,
                    null,
                    order++,
                    null, null, null, null, null, null,
                    String.format("%s||추천 행사 주변 맛집입니다. (%s 취향 반영)", restaurant.placeName(), userPreferenceText)
            );
            course.addCoursePlace(restaurantCoursePlace);
        }

        // ③ 주변 추천 카페 추가
        if (cafe != null) {
            CoursePlace cafeCoursePlace = CoursePlace.create(
                    course,
                    null,
                    null,
                    order++,
                    null, null, null, null, null, null,
                    String.format("%s||코스의 마무리를 장식할 추천 카페입니다. (%s 취향 반영)", cafe.placeName(), userPreferenceText)
            );
            course.addCoursePlace(cafeCoursePlace);
        }

        courseRepository.save(course);
        return course.getId();
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
                            String reason = "추천된 장소입니다.";

                            if (cp.getPlace() != null) {
                                placeId = cp.getPlace().getId();
                            } else if (cp.getEvent() != null && cp.getEvent().getPlace() != null) {
                                placeId = cp.getEvent().getPlace().getId();
                            }

                            // 💡 11번째 파라미터인 getRecommendationReason()을 안전하게 파싱
                            if (cp.getRecommendationReason() != null && cp.getRecommendationReason().contains("||")) {
                                String[] split = cp.getRecommendationReason().split("\\|\\|");
                                placeName = split[0];
                                reason = split[1];
                            } else if (cp.getEvent() != null) {
                                placeName = cp.getEvent().getTitle();
                            } else if (cp.getPlace() != null) {
                                placeName = cp.getPlace().getName();
                            }

                            return new CoursePlaceResponse(
                                    placeId,
                                    placeName,
                                    cp.getVisitOrder(),
                                    reason
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