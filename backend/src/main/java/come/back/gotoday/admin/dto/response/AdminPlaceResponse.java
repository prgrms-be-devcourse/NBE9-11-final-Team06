package come.back.gotoday.admin.dto.response;

import come.back.gotoday.place.entity.Place;

import java.math.BigDecimal;

public record AdminPlaceResponse(
        Long id,
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
        String externalId,
        Boolean isActive
) {

    public static AdminPlaceResponse from(Place place) {
        return new AdminPlaceResponse(
                place.getId(),
                place.getCategory().getId(),
                place.getName(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPhone(),
                place.getPlaceUrl(),
                place.getDescription(),
                place.getSource(),
                place.getExternalId(),
                place.getIsActive()
        );
    }
}