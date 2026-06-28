package come.back.gotoday.admin.controller;

import come.back.gotoday.admin.service.AdminCrowdRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/crowds")
@RequiredArgsConstructor
public class AdminCrowdController {

    private final AdminCrowdRefreshService adminCrowdRefreshService;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshAllCrowdStatuses() {
        log.info("관리자 혼잡도 전체 갱신 요청 수신");

        boolean started = adminCrowdRefreshService.startRefresh();

        if (!started) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.accepted().build();
    }
}