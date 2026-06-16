package come.back.gotoday.recommend.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.recommend.dto.RecommendationCourseCreateRequest;
import come.back.gotoday.recommend.dto.RecommendationCourseResponse;
import come.back.gotoday.recommend.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<RecommendationCourseResponse>> createRecommendedCourse(
            @AuthenticationPrincipal(expression = "memberId") Long memberId,
            @Valid @RequestBody RecommendationCourseCreateRequest request
    ) {
        RecommendationCourseResponse response = recommendationService.createRecommendedCourse(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "추천 코스 생성 성공"));
    }
}
