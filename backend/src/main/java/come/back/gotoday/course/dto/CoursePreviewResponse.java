package come.back.gotoday.course.dto;

import come.back.gotoday.external.kakao.dto.KakaoPlaceDocument;

import java.util.List;

public record CoursePreviewResponse(

        List<Long> eventIds,
        List<EventNearbyPlaceResponse> events,
        Double startLatitude,
        Double startLongitude

) {
}