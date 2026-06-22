package come.back.gotoday.course.dto;

import come.back.gotoday.course.type.RestaurantType;

import java.time.LocalDate;

public record CoursePreviewRequest(
        String courseType,
        LocalDate startDate,
        LocalDate endDate,
        String baseArea,
        String companionType,

        // 시작 위치 추가 (검색의 기준이 되는)
        Double startLatitude,
        Double startLongitude,

        RestaurantType restaurantType
) {
}