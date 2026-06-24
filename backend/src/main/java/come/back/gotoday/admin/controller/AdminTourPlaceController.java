package come.back.gotoday.admin.controller;

import come.back.gotoday.external.tour.dto.TourPlaceSyncAcceptedResponse;
import come.back.gotoday.external.tour.service.TourPlaceSyncAsyncService;
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

    private final TourPlaceSyncAsyncService tourPlaceSyncAsyncService;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<TourPlaceSyncAcceptedResponse>> syncTourPlaces(
            @RequestParam(defaultValue = "1") String areaCode
    ) {
        log.info("관리자 관광공사 관광지 동기화 요청 접수: areaCode={}", areaCode);

        tourPlaceSyncAsyncService.syncTourPlacesAsync(areaCode);

        TourPlaceSyncAcceptedResponse response = new TourPlaceSyncAcceptedResponse(
                areaCode,
                "ACCEPTED",
                "관광공사 관광지 동기화 요청이 접수되었습니다."
        );

        return ResponseEntity.accepted()
                .body(ApiResponse.success(response, "관광공사 관광지 동기화 요청 접수"));
    }
}