package come.back.gotoday.course.controller;

import come.back.gotoday.course.dto.*;
import come.back.gotoday.course.service.CourseService;
import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // 코스 생성
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCourse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CourseCreateRequest request
    ) {
        log.info(
                "코스 생성 요청: memberId={}, title={}, eventCount={}",
                userDetails.getMemberId(),
                request.title(),
                request.eventIds() != null ? request.eventIds().size() : 0
        );

        Long courseId = courseService.createCourse(
                userDetails.getMemberId(),
                request
        );

        log.info(
                "코스 생성 응답: memberId={}, courseId={}",
                userDetails.getMemberId(),
                courseId
        );

        return ResponseEntity.ok(
                ApiResponse.success(courseId, "코스 생성에 성공했습니다.")
        );
    }

    // 코스 프리뷰 (식당/카페 미리 추천)
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<CoursePreviewResponse>> previewCourse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CoursePreviewRequest request
    ) {
        log.info(
                "코스 프리뷰 요청: memberId={}, baseArea={}",
                userDetails.getMemberId(),
                request.baseArea()
        );

        CoursePreviewResponse response = courseService.previewCourse(
                userDetails.getMemberId(),
                request
        );

        log.info(
                "코스 프리뷰 응답: memberId={}",
                userDetails.getMemberId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(response, "코스 프리뷰 생성에 성공했습니다.")
        );
    }

    // 코스 북마크 등록/해제
    @PostMapping("/{courseId}/bookmark")
    public ResponseEntity<ApiResponse<CourseBookmarkResponse>> toggleBookmark(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId
    ) {
        log.info(
                "코스 북마크 토글 요청: memberId={}, courseId={}",
                userDetails.getMemberId(),
                courseId
        );

        CourseBookmarkResponse response = courseService.toggleBookmark(
                userDetails.getMemberId(),
                courseId
        );

        String message = response.bookmarked()
                ? "코스 북마크가 등록되었습니다."
                : "코스 북마크가 해제되었습니다.";

        log.info(
                "코스 북마크 토글 응답: memberId={}, courseId={}, bookmarked={}",
                userDetails.getMemberId(),
                courseId,
                response.bookmarked()
        );

        return ResponseEntity.ok(
                ApiResponse.success(response, message)
        );
    }

    // 코스 북마크 여부 조회
    @GetMapping("/{courseId}/bookmark")
    public ResponseEntity<ApiResponse<Boolean>> isBookmarked(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId
    ) {
        log.info(
                "코스 북마크 여부 조회 요청: memberId={}, courseId={}",
                userDetails.getMemberId(),
                courseId
        );

        boolean bookmarked = courseService.isBookmarked(
                userDetails.getMemberId(),
                courseId
        );

        log.info(
                "코스 북마크 여부 조회 응답: memberId={}, courseId={}, bookmarked={}",
                userDetails.getMemberId(),
                courseId,
                bookmarked
        );

        return ResponseEntity.ok(
                ApiResponse.success(bookmarked, "코스 북마크 여부 조회에 성공했습니다.")
        );
    }

    // 내가 북마크한 코스 목록 조회
    @GetMapping("/bookmarks")
    public ResponseEntity<ApiResponse<List<SavedCourseResponse>>> getSavedCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info(
                "북마크한 코스 목록 조회 요청: memberId={}",
                userDetails.getMemberId()
        );

        List<SavedCourseResponse> response = courseService.getSavedCourses(
                userDetails.getMemberId()
        );

        log.info(
                "북마크한 코스 목록 조회 응답: memberId={}, resultCount={}",
                userDetails.getMemberId(),
                response.size()
        );

        return ResponseEntity.ok(
                ApiResponse.success(response, "북마크한 코스 목록 조회에 성공했습니다.")
        );
    }

    // 코스 단건 조회
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> getCourse(
            @PathVariable Long courseId
    ) {
        log.info("코스 단건 조회 요청: courseId={}", courseId);

        CourseDetailResponse response = courseService.getCourse(courseId);

        log.info("코스 단건 조회 응답: courseId={}", courseId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "코스 조회에 성공했습니다.")
        );
    }

    // 코스 다건 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseListResponse>>> getCourses() {
        log.info("코스 목록 조회 요청");

        List<CourseListResponse> response = courseService.getCourses();

        log.info("코스 목록 조회 응답: resultCount={}", response.size());

        return ResponseEntity.ok(
                ApiResponse.success(response, "코스 목록 조회에 성공했습니다.")
        );
    }

    // 코스 삭제
    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId
    ) {
        log.info(
                "코스 삭제 요청: memberId={}, courseId={}",
                userDetails.getMemberId(),
                courseId
        );

        courseService.deleteCourse(userDetails.getMemberId(), courseId);

        log.info(
                "코스 삭제 응답: memberId={}, courseId={}",
                userDetails.getMemberId(),
                courseId
        );

        return ResponseEntity.ok(
                ApiResponse.success(null, "코스 삭제에 성공했습니다.")
        );
    }
}