package come.back.gotoday.place.dto;

import java.math.BigDecimal;

public record PlaceResponse(
        Long id,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Long categoryId
) {
}