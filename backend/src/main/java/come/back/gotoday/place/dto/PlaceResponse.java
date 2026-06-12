package come.back.gotoday.place.dto;

public record PlaceResponse(
        Long id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String category
) {
}