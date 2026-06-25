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

    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 10;

    public int getTopKOrDefault() {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }

        return Math.min(topK, MAX_TOP_K);
    }

    public String getAreaOrDefault() {
        if (area != null && !area.isBlank()) {
            return area;
        }

        if (baseArea != null && !baseArea.isBlank()) {
            return baseArea;
        }

        return null;
    }
}