package come.back.gotoday.course.dto;

import come.back.gotoday.course.type.CourseItemType;

import java.time.LocalDate;
import java.time.LocalTime;

public record CoursePlaceRequest(
        CourseItemType itemType,
        Long placeId,
        Long eventId,
        Long tourId,
        Integer visitOrder,
        LocalDate visitDate,
        LocalTime startTime,
        LocalTime endTime,
        String recommendationReason
) {
}