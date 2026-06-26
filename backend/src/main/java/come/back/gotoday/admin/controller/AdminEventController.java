package come.back.gotoday.admin.controller;

import come.back.gotoday.event.dto.EventListResponse;
import come.back.gotoday.event.dto.EventSearchRequest;
import come.back.gotoday.event.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventListResponse>> getEvents(
            @ModelAttribute EventSearchRequest request
    ) {
        log.info(
                "관리자 행사 목록 조회 요청: area={}, categoryId={}, keyword={}, status={}, page={}",
                request.area(),
                request.categoryId(),
                request.keyword(),
                request.status(),
                request.page()
        );

        Page<EventListResponse> response = eventService.getEventList(request);

        return ResponseEntity.ok(response);
    }
}