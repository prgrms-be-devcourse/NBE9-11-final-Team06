package come.back.gotoday.event.controller;

import come.back.gotoday.event.dto.EventDetailResponse;
import come.back.gotoday.event.dto.EventListResponse;
import come.back.gotoday.event.dto.EventSearchRequest;
import come.back.gotoday.event.service.EventService;
import come.back.gotoday.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // 이벤트 단건(상세) 조회
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> getEvent(
            @PathVariable Long eventId
    ) {
        log.info("이벤트 단건 조회 요청: eventId={}", eventId);

        EventDetailResponse response = eventService.getEvent(eventId);

        log.info("이벤트 단건 조회 응답: eventId={}", eventId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "이벤트 조회에 성공했습니다.")
        );
    }

    // 이벤트 다건(목록) 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventListResponse>>> getEvents(
            @ModelAttribute EventSearchRequest request
    ) {
        log.info("이벤트 검색/필터 목록 조회 요청: area={}, categoryId={}, keyword={}, status={}, page={}",
                request.area(), request.categoryId(), request.keyword(), request.status(), request.page());

        Page<EventListResponse> response = eventService.getEventList(request);

        log.info("이벤트 목록 검색 완료: 결과 건수={}", response.getNumberOfElements());
        return ResponseEntity.ok(
                ApiResponse.success(response, "이벤트 목록 검색에 성공했습니다.")
        );
    }
}