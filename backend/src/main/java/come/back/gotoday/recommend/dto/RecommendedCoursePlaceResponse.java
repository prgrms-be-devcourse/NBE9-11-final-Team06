package come.back.gotoday.recommend.dto;

import come.back.gotoday.course.type.CourseItemType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecommendedCoursePlaceResponse(
        CourseItemType itemType,
        Long eventId,
        Long placeId,
        Long tourId,
        String title,
        String categoryName,
        String address,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer visitOrder,
        String recommendationReason
) {
}