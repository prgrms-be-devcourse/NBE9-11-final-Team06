package come.back.gotoday.recommend.dto;

import java.math.BigDecimal;

public record TourPlacePreviewResponse(
        Long placeId,
        String title,
        String categoryName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String url,
        String recommendationReason
) {
}