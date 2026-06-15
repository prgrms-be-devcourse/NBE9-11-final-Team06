package come.back.gotoday.recommend.dto;

import java.time.LocalDate;
import java.util.List;

public record RecommendationCourseResponse(
        Long courseId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<RecommendedCoursePlaceResponse> places
) {
}
