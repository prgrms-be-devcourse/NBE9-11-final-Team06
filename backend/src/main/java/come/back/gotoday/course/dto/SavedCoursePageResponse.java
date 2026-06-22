package come.back.gotoday.course.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record SavedCoursePageResponse(
        List<SavedCourseResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static SavedCoursePageResponse from(Page<SavedCourseResponse> page) {
        return new SavedCoursePageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}