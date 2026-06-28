package come.back.gotoday.review.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.review.dto.ReviewCreateRequest;
import come.back.gotoday.review.dto.ReviewResponse;
import come.back.gotoday.review.dto.ReviewUpdateRequest;
import come.back.gotoday.review.service.ReviewService;
import jakarta.validation.Valid;
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
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 생성
    @PostMapping("/{courseId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long courseId,
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ReviewResponse response = reviewService.createReview(
                courseId,
                userDetails.getMemberId(),
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(response, "리뷰 생성에 성공했습니다.")
        );
    }

    // 리뷰 단건 조회 (해당 코스에서 본인이 작성한 글 조회)
    @GetMapping("/{courseId}/reviews/me")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ReviewResponse response = reviewService.getReview(
                courseId,
                userDetails.getMemberId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(response, "리뷰 조회에 성공했습니다.")
        );
    }

    // 리뷰 다건 조회 (코스 기준)
    @GetMapping("/{courseId}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviews(
            @PathVariable Long courseId) {

        List<ReviewResponse> response = reviewService.getReviews(courseId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "리뷰 목록 조회에 성공했습니다.")
        );
    }

    // 리뷰 수정
    @PatchMapping("/{courseId}/reviews/me")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long courseId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ReviewResponse response = reviewService.updateReview(
                courseId,
                userDetails.getMemberId(),
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(response, "리뷰 수정에 성공했습니다.")
        );
    }

    // 리뷰 삭제
    @DeleteMapping("/{courseId}/reviews/me")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        reviewService.deleteReview(
                courseId,
                userDetails.getMemberId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(null, "리뷰 삭제에 성공했습니다.")
        );
    }
}