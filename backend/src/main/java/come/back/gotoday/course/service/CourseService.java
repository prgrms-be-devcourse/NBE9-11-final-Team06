package come.back.gotoday.course.service;

import come.back.gotoday.course.dto.CourseCreateRequest;
import come.back.gotoday.course.dto.CourseDetailResponse;
import come.back.gotoday.course.dto.CourseListResponse;
import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.repository.CoursePlaceRepository;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
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

    //코스저장
    @Transactional
    public Long createCourse(
            Long memberId,
            CourseCreateRequest request
    ) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(
                        () -> new IllegalArgumentException("회원이 존재하지 않습니다.")
                );

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

        courseRepository.save(course);

        return course.getId();
    }

    //코스 상세조회 - 조회만 하기 때문에 readOnly
    public CourseDetailResponse getCourse(
            Long courseId
    ) {

        Course course =
                courseRepository.findById(courseId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "존재하지 않는 코스입니다."
                                )
                        );

        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCourseType(),
                course.getBaseArea(),
                course.getCompanionType(),
                List.of()
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
                        course.getBaseArea(),
                        course.getCourseType(),
                        course.getStartDate()
                ))
                .toList();
    }

    @Transactional
    public void deleteCourse(
            Long courseId
    ) {

        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException(
                    "존재하지 않는 코스입니다."
            );
        }

        courseRepository.deleteById(courseId);
    }


}