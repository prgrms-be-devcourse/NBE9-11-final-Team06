package come.back.gotoday.admin.controller;

import come.back.gotoday.admin.dto.response.AdminSyncResponse;
import come.back.gotoday.admin.service.AdminSyncService;
import come.back.gotoday.global.response.ApiResponse;
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

}