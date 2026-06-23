package come.back.gotoday.course.dto;


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
        Double startLatitude,
        Double startLongitude,
        List<Long> eventIds,
        Long restaurantId,
        Long cafeId
) {
}