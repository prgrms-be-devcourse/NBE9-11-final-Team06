package come.back.gotoday.course.service;

import come.back.gotoday.course.dto.*;
import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.entity.CoursePlace;
import come.back.gotoday.course.repository.CoursePlaceRepository;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;

    //코스저장
    @Transactional
    public Long createCourse(Long memberId, CourseCreateRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

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

        int order = 1;
        for (Long placeId : request.placeIds()) {
            Place place = placeMap.get(placeId);
            if (place == null) {
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

        return course.getId();
    }

    //코스 상세조회 - 조회만 하기 때문에 readOnly
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourse(Long courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

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

    //코스 삭제
    @Transactional
    public void deleteCourse(
            Long memberId,
            Long courseId
    ) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if (!course.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 코스를 삭제할 권한이 없습니다.");
        }

        coursePlaceRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
    }

}