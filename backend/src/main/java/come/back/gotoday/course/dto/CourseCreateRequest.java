package come.back.gotoday.course.dto;


import java.time.LocalDate;
import java.util.List;

public record CourseCreateRequest(
        String title,
        String description,
        String courseType,
        LocalDate startDate,
        LocalDate endDate,
        String baseArea,
        String companionType,
        List<Long> eventIds,
        Long restaurantId,
        Long cafeId,
        //지도에 표시할 시작 위경도
        Double startLatitude,
        Double startLongitude
) {
}