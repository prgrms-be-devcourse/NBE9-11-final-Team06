package come.back.gotoday.recommend.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.recommend.dto.RecommendationCourseCreateRequest;
import come.back.gotoday.recommend.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping("/candidates")
    public ResponseEntity<ApiResponse<RecommendationService.RecommendationCandidateDraft>> previewRecommendationCandidates(
            @AuthenticationPrincipal(expression = "memberId") Long memberId,
            @Valid @RequestBody RecommendationCourseCreateRequest request
    ) {
        RecommendationService.RecommendationCandidateDraft response =
                recommendationService.recommendCandidates(memberId, request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "추천 후보 조회 성공")
        );
    }

}
