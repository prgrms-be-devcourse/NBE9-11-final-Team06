package come.back.gotoday.course.controller;

import come.back.gotoday.course.dto.CourseCreateRequest;
import come.back.gotoday.course.dto.CourseDetailResponse;
import come.back.gotoday.course.dto.CourseListResponse;
import come.back.gotoday.course.service.CourseService;
import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    //코스 생성 (로그인 token 과 + json 필요)
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCourse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CourseCreateRequest request
    ) {

        Long courseId =
                courseService.createCourse(
                        userDetails.getMemberId(),
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(courseId)
        );
    }

    //코스 단건 조회
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseDetailResponse>>
    getCourse(
            @PathVariable Long courseId
    ) {

        CourseDetailResponse response =
                courseService.getCourse(courseId);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }


    //코스 다건 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseListResponse>>>
    getCourses() {

        List<CourseListResponse> response =
                courseService.getCourses();

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    //코스 삭제
    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>>
    deleteCourse(
            @PathVariable Long courseId
    ) {

        courseService.deleteCourse(courseId);

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }
}