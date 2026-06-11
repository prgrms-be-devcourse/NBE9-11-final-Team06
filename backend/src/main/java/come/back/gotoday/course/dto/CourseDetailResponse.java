package come.back.gotoday.course.dto;

import java.util.List;

public record CourseDetailResponse(
        Long courseId,
        String title,
        String description,
        String courseType,
        String baseArea,
        String companionType,
        List<CoursePlaceResponse> places
) {
}