package come.back.gotoday.admin.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.tour.dto.TourSyncResponse;
import come.back.gotoday.tour.service.TourSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tours")
public class AdminTourController {

    private final TourSyncService tourSyncService;

    @PostMapping("/sync/seoul")
    public ApiResponse<TourSyncResponse> syncAllSeoulTours() {
        int syncedCount = tourSyncService.syncAllSeoulTours();

        return ApiResponse.success(
                new TourSyncResponse(syncedCount),
                "서울 전체 관광지 데이터 동기화에 성공했습니다."
        );
    }
}