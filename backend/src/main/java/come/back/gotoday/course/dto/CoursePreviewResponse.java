package come.back.gotoday.course.dto;

import java.util.List;

public record CoursePreviewResponse(

        List<Long> eventIds,
        List<Long> tourIds,
        List<EventNearbyPlaceResponse> events,
        Double startLatitude,
        Double startLongitude

) {
}