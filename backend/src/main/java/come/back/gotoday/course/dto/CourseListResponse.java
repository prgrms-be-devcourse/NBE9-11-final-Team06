package come.back.gotoday.course.dto;

import java.time.LocalDate;

public record CourseListResponse(
        Long courseId,
        String title,
        String baseArea,
        String courseType,
        LocalDate startDate
) {
}