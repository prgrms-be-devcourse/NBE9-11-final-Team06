package come.back.gotoday.recommend.dto;

import come.back.gotoday.tour.entity.Tour;

public record TourPlacePreviewResponse(
        Long tourId,
        String title,
        String categoryName,
        String address,
        Double latitude,
        Double longitude,
        String url,
        String recommendationReason
) {

    public static TourPlacePreviewResponse from(
            Tour tour,
            String recommendationReason
    ) {
        return new TourPlacePreviewResponse(
                tour.getId(),
                tour.getTitle(),
                getCategoryName(tour),
                tour.getAddress(),
                tour.getLatitude(),
                tour.getLongitude(),
                tour.getHomepageUrl(),
                recommendationReason
        );
    }

    private static String getCategoryName(Tour tour) {
        if (tour.getCategory() == null) {
            return "관광지";
        }

        return tour.getCategory().getName();
    }
}