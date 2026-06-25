package come.back.gotoday.admin.controller;

import come.back.gotoday.admin.dto.request.AdminKakaoPlaceSyncRequest;
import come.back.gotoday.admin.dto.response.AdminSyncResponse;
import come.back.gotoday.admin.service.AdminSyncService;
import come.back.gotoday.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sync")
public class AdminSyncController {

    private final AdminSyncService adminSyncService;

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<AdminSyncResponse>> syncEvents() {
        log.info("관리자 서울시 행사 동기화 요청 접수");

        AdminSyncResponse response = adminSyncService.syncEvents();

        return ResponseEntity.ok(
                ApiResponse.success(response, "서울시 행사 동기화 완료")
        );
    }

    @PostMapping("/tour-places")
    public ResponseEntity<ApiResponse<AdminSyncResponse>> syncTourPlaces(
            @RequestParam(defaultValue = "1") String areaCode
    ) {
        log.info("관리자 관광공사 관광지 동기화 요청 접수: areaCode={}", areaCode);

        AdminSyncResponse response = adminSyncService.syncTourPlaces(areaCode);

        return ResponseEntity.accepted()
                .body(ApiResponse.success(response, "관광공사 관광지 동기화 요청 접수"));
    }

    @PostMapping("/kakao-places")
    public ResponseEntity<ApiResponse<AdminSyncResponse>> syncKakaoPlaces(
            @RequestParam(defaultValue = "30") int limit
    ) {
        log.info("관리자 카카오 장소 DB 기준 동기화 요청 접수: limit={}", limit);

        AdminSyncResponse response = adminSyncService.syncKakaoPlaces(limit);

        return ResponseEntity.ok(
                ApiResponse.success(response, "카카오 장소 동기화 완료")
        );
    }

    @PostMapping("/kakao-places/nearby")
    public ResponseEntity<ApiResponse<AdminSyncResponse>> syncKakaoPlacesNearby(
            @Valid @RequestBody AdminKakaoPlaceSyncRequest request
    ) {
        log.info(
                "관리자 카카오 장소 좌표 기반 동기화 요청 접수: latitude={}, longitude={}",
                request.latitude(),
                request.longitude()
        );

        AdminSyncResponse response = adminSyncService.syncKakaoPlacesNearby(request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "카카오 장소 좌표 기반 동기화 완료")
        );
    }
}