package come.back.gotoday.course.dto;

import java.time.LocalDate;
import java.util.List;

public record CourseDetailResponse(
        Long courseId,
        String title,
        String description,
        String courseType,
        LocalDate startDate,
        LocalDate endDate,
        String baseArea,
        String companionType,

        Double startLatitude,
        Double startLongitude,

        List<CoursePlaceResponse> places,
        double totalDistance,
        int estimatedTime
) {
}