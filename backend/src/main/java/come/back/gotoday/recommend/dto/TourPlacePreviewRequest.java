package come.back.gotoday.recommend.dto;

import java.util.List;

public record TourPlacePreviewRequest(
        String area,
        String baseArea,
        Integer topK,
        List<String> categories,
        Double startLatitude,
        Double startLongitude
) {

    public String getAreaOrDefault() {
        if (baseArea != null && !baseArea.isBlank()) {
            return baseArea;
        }

        if (area != null && !area.isBlank()) {
            return area;
        }

        return "";
    }

    public int getTopKOrDefault() {
        if (topK == null || topK <= 0) {
            return 3;
        }

        return topK;
    }
}