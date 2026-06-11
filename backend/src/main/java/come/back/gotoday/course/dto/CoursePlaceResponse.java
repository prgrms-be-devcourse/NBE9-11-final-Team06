package come.back.gotoday.course.dto;


public record CoursePlaceResponse(
        Long placeId,
        String placeName,
        Integer visitOrder,
        String recommendationReason
) {
}