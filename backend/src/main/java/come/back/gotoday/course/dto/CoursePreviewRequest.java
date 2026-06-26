package come.back.gotoday.course.dto;

import come.back.gotoday.course.type.RestaurantType;

import java.time.LocalDate;
import java.util.List;

public record CoursePreviewRequest(
        String courseType,
        LocalDate startDate,
        LocalDate endDate,
        String baseArea,
        List<String> categories,
        String companionType,
        RestaurantType restaurantType,
        Double startLatitude,
        Double startLongitude,
        List<Long> eventIds,
        List<Long> tourIds
) {
}