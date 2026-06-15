package come.back.gotoday.recommend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecommendedCoursePlaceResponse(
        Long eventId,
        Long placeId,
        String title,
        String categoryName,
        String area,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal latitude,
        BigDecimal longitude,
        int visitOrder,
        String recommendationReason
) {
}
