package come.back.gotoday.course.dto;

public record CourseBookmarkResponse(
        Long courseId,
        boolean bookmarked
) {
    public static CourseBookmarkResponse bookmarked(Long courseId) {
        return new CourseBookmarkResponse(courseId, true);
    }

    public static CourseBookmarkResponse unbookmarked(Long courseId) {
        return new CourseBookmarkResponse(courseId, false);
    }
}
