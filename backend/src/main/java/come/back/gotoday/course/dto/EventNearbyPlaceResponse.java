package come.back.gotoday.course.dto;

import java.util.List;

public record EventNearbyPlaceResponse(
        Long eventId,
        String eventTitle,
        List<PlacePreviewResponse> restaurants,
        List<PlacePreviewResponse> cafes
) {
}