package come.back.gotoday.course.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record CoursePlaceRequest(
        Long placeId,
        Integer visitOrder,
        LocalDate visitDate,
        LocalTime startTime,
        LocalTime endTime,
        String recommendationReason
) {
}