package come.back.gotoday.admin.controller;

import come.back.gotoday.external.tour.dto.TourPlaceSyncResponse;
import come.back.gotoday.external.tour.service.TourPlaceSyncService;
import come.back.gotoday.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tour-places")
public class AdminTourPlaceController {

    private final TourPlaceSyncService tourPlaceSyncService;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<TourPlaceSyncResponse>> syncTourPlaces(
            @RequestParam(defaultValue = "1") String areaCode
    ) {
        log.info("관리자 관광공사 관광지 동기화 요청: areaCode={}", areaCode);

        int syncedCount = tourPlaceSyncService.syncTourPlaces(areaCode);

        TourPlaceSyncResponse response = new TourPlaceSyncResponse(areaCode, syncedCount);

        return ResponseEntity.ok(
                ApiResponse.success(response, "관광공사 관광지 동기화 성공")
        );
    }
}