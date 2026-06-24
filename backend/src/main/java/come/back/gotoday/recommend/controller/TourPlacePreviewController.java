package come.back.gotoday.recommend.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.recommend.dto.TourPlacePreviewRequest;
import come.back.gotoday.recommend.dto.TourPlacePreviewResponse;
import come.back.gotoday.recommend.service.TourPlacePreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations/tour-places")
public class TourPlacePreviewController {

    private final TourPlacePreviewService tourPlacePreviewService;

    @PostMapping("/preview")
    public ApiResponse<List<TourPlacePreviewResponse>> previewTourPlaces(
            @RequestBody TourPlacePreviewRequest request
    ) {
        List<TourPlacePreviewResponse> response =
                tourPlacePreviewService.previewTourPlaces(request);

        return ApiResponse.success(response, "관광지 추천 미리보기에 성공했습니다.");
    }
}