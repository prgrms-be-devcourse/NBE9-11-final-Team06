package come.back.gotoday.course.dto;

import come.back.gotoday.course.type.CourseItemType;

import java.math.BigDecimal;

public record CoursePlaceResponse(
        CourseItemType itemType,
        Long placeId,
        Long eventId,
        Long tourId,
        String itemName,
        Integer visitOrder,
        String recommendationReason,
        BigDecimal latitude,
        BigDecimal longitude,
        String address
) {
}