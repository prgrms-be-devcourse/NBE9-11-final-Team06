package come.back.gotoday.event.service;

import come.back.gotoday.event.dto.EventDetailResponse;
import come.back.gotoday.event.dto.EventListResponse;
import come.back.gotoday.event.dto.EventSearchRequest;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    /**
     * 이벤트 상세 조회
     */
    public EventDetailResponse getEvent(Long eventId) {
        log.info("EventService - 이벤트 상세 조회 시작: eventId={}", eventId);

        Event event = eventRepository.findByIdWithFetch(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        log.info("EventService - 이벤트 상세 조회 완료: eventId={}, title={}", eventId, event.getTitle());
        return EventDetailResponse.from(event);
    }

    public Page<EventListResponse> getEventList(EventSearchRequest request) {
        LocalDate now = LocalDate.now();

        // 빈 문자열이 오면 쿼리에서 null로 인식하도록 방어코드 작성
        String area = (request.area() != null && !request.area().isBlank()) ? request.area() : null;
        String keyword = (request.keyword() != null && !request.keyword().isBlank()) ? request.keyword() : null;

        String statusStr = (request.status() != null) ? request.status().name() : null;
        Pageable pageable = PageRequest.of(request.page(), request.size(), Sort.by(Sort.Direction.DESC, "startDate"));

        Page<Event> eventPage = eventRepository.findEventsByFilters(
                area, request.categoryId(), keyword, statusStr, now, pageable
        );

        return eventPage.map(EventListResponse::from);
    }
}