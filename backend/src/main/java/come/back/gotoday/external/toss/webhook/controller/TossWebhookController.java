package come.back.gotoday.external.toss.webhook.controller;

import come.back.gotoday.external.toss.webhook.dto.TossWebhookRequest;
import come.back.gotoday.external.toss.webhook.service.TossWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class TossWebhookController {

    private final TossWebhookService tossWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveTossWebhook(@RequestBody TossWebhookRequest request) {

        try {
            tossWebhookService.handleWebhook(request);
            // 토스 웹훅 서버에게 정상 수신했음을 알림 (200 OK)
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("[토스 웹훅 내부 에러] 웹훅 데이터 정합성 처리 중 예외 발생: {}", e.getMessage(), e);

            return ResponseEntity.internalServerError().build();
        }
    }
}