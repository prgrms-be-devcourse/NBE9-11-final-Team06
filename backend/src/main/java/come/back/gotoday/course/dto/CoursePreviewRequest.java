package come.back.gotoday.course.dto;

import come.back.gotoday.course.type.RestaurantType;

import java.time.LocalDate;

public record CoursePreviewRequest(
        String courseType,
        LocalDate startDate,
        LocalDate endDate,
        String baseArea,
        String companionType,
        RestaurantType restaurantType,
        Double startLatitude,
        Double startLongitude
) {
}