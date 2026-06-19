package come.back.gotoday.course.dto;

public record PlacePreviewResponse(
        Long id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String url
) {}