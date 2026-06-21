package come.back.gotoday.course.dto;


import java.math.BigDecimal;

public record CoursePlaceResponse(
        Long placeId,
        String placeName,
        Integer visitOrder,
        String recommendationReason,
        BigDecimal latitude,
        BigDecimal longitude,
        String address
) {
}