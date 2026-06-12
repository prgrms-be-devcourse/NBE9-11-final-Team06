package come.back.gotoday.place.dto;

import java.math.BigDecimal;

public record PlaceCreateRequest(
        Long categoryId,
        String name,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone,
        String placeUrl,
        String description,
        String source,
        String externalId
) {
}