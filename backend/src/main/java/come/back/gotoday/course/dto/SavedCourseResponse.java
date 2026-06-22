package come.back.gotoday.course.dto;

import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.entity.SavedCourse;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SavedCourseResponse(
        Long savedCourseId,
        Long courseId,
        String title,
        String courseType,
        String baseArea,
        LocalDate startDate,
        LocalDateTime savedAt
) {

    public static SavedCourseResponse from(SavedCourse savedCourse) {
        Course course = savedCourse.getCourse();

        return new SavedCourseResponse(
                savedCourse.getId(),
                course.getId(),
                course.getTitle(),
                course.getCourseType() != null ? String.valueOf(course.getCourseType()) : null,
                course.getBaseArea(),
                course.getStartDate(),
                savedCourse.getCreatedAt()
        );
    }
}