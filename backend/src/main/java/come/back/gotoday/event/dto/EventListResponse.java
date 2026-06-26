package come.back.gotoday.event.dto;

import come.back.gotoday.event.entity.Event;
import java.time.LocalDate;

public record EventListResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String eventTime,
        String area,
        String imageUrl,
        String categoryName,
        String eventCategory
) {
    public static EventListResponse from(Event event) {
        return new EventListResponse(
                event.getId(),
                event.getTitle(),
                event.getStartDate(),
                event.getEndDate(),
                event.getEventTime(),
                event.getArea(),
                event.getImageUrl(),
                event.getCategory().getName(),
                event.getEventCategory()
        );
    }
}