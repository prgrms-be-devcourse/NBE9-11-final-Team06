package come.back.gotoday.course.dto;

import java.util.List;

public record EventNearbyPlaceResponse(
        String itemType,
        Long itemId,
        String title,
        Double latitude,
        Double longitude,
        List<PlacePreviewResponse> restaurants,
        List<PlacePreviewResponse> cafes
) {
}