package come.back.gotoday.event.dto;

import come.back.gotoday.event.entity.Event;
import java.time.LocalDate;

public record EventDetailResponse(
        Long id,
        Long placeId,
        String placeName,
        Long categoryId,
        String categoryName,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String eventTime,
        String fee,
        String target,
        String homepageUrl,
        String imageUrl,
        String description,
        String source,
        String area,
        Double latitude,
        Double longitude
) {
    public static EventDetailResponse from(Event event) {
        return new EventDetailResponse(
                event.getId(),
                event.getPlace() != null ? event.getPlace().getId() : null,
                event.getPlace() != null ? event.getPlace().getName() : null, // 필요시 수정
                event.getCategory().getId(),
                event.getCategory().getName(), // 필요시 수정
                event.getTitle(),
                event.getStartDate(),
                event.getEndDate(),
                event.getEventTime(),
                event.getFee(),
                event.getTarget(),
                event.getHomepageUrl(),
                event.getImageUrl(),
                event.getDescription(),
                event.getSource(),
                event.getArea(),
                event.getLatitude(),
                event.getLongitude()
        );
    }
}