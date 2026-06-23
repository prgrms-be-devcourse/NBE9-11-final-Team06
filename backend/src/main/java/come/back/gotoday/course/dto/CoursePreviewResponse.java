package come.back.gotoday.course.dto;

import come.back.gotoday.external.kakao.dto.KakaoPlaceDocument;

import java.util.List;

public record CoursePreviewResponse(

        List<Long> eventIds,

        List<PlacePreviewResponse> restaurants,

        List<PlacePreviewResponse> cafes,

        Double startLatitude,

        Double startLongitude

) {
}