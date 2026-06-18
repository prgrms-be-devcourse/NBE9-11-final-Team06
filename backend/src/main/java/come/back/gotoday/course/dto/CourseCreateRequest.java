package come.back.gotoday.course.dto;

import come.back.gotoday.course.type.RestaurantType;

import java.time.LocalDate;
import java.util.List;

public record CourseCreateRequest(
        String title,
        String description,
        String courseType,
        LocalDate startDate,
        LocalDate endDate,
        String baseArea,
        String companionType,
        List<Long> placeIds,
        RestaurantType restaurantType
) {
}