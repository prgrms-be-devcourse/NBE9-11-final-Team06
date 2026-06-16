package come.back.gotoday.event.dto;

import come.back.gotoday.event.enums.EventStatus;

public record EventSearchRequest(
        String area,       // 자치구 필터 (ex. "영등포구", "마포구")
        Long categoryId,   // 특정 카테고리 고유 ID 필터 (ex. 14[콘서트], 16[클래식])
        String keyword,    // 제목 검색 키워드 (ex. "금난새")
        EventStatus status,     // 진행 상황 필터 ("ING": 진행중, "END": 마감)
        Integer page,
        Integer size
) {
    public EventSearchRequest {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0) size = 10;
    }
}