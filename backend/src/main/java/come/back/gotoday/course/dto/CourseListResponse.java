package come.back.gotoday.course.dto;

import java.time.LocalDate;

public record CourseListResponse(
        Long courseId,
        String title,
        String courseType,
        String baseArea,
        LocalDate startDate,
        double averageRating,
        int reviewCount
) {}