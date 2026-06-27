package come.back.gotoday.course.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record CourseCreateRequest(
        String title,
        String description,
        String courseType,
        LocalDate startDate,
        LocalDate endDate,
        String baseArea,
        String companionType,
        Double startLatitude,
        Double startLongitude,
        List<Long> eventIds,
        List<Long> tourIds,
        Long restaurantId,
        Long cafeId,
        List<SelectedRecommendationItem> selectedRecommendationItems
) {
    public List<SelectedRecommendationItem> getSelectedRecommendationItemsOrEmpty() {
        return selectedRecommendationItems == null
                ? List.of()
                : selectedRecommendationItems.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    public record SelectedRecommendationItem(
            String itemType,
            Long eventId,
            Long tourId,
            String recommendationReason
    ) {
        public boolean isEvent() {
            return "EVENT".equals(itemType) && eventId != null && tourId == null;
        }

        public boolean isTour() {
            return "TOUR".equals(itemType) && tourId != null && eventId == null;
        }
    }
}