package come.back.gotoday.event.controller;

import come.back.gotoday.event.service.EventBatchService;
import come.back.gotoday.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events/batch")
public class EventBatchController {

    private final EventBatchService eventBatchService;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Void>> syncSeoulEvents() {
        eventBatchService.syncSeoulEvents();
        return ResponseEntity.ok(ApiResponse.success(null, "서울시 행사 데이터 동기화 성공"));
    }
}
