package come.back.gotoday.admin.controller;

import come.back.gotoday.crowd.service.CrowdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/crowds")
@RequiredArgsConstructor
public class AdminCrowdController {

    private final CrowdService crowdService;

    @PostMapping("/refresh")
    public ResponseEntity<CrowdService.CrowdCollectionResult> refreshAllCrowdStatuses() {
        log.info("관리자 혼잡도 전체 갱신 요청 수신");

        CrowdService.CrowdCollectionResult result = crowdService.refreshAllCrowdStatuses();

        log.info(
                "관리자 혼잡도 전체 갱신 완료: successCount={}, failureCount={}",
                result.successCount(),
                result.failureCount()
        );

        return ResponseEntity.ok(result);
    }
}