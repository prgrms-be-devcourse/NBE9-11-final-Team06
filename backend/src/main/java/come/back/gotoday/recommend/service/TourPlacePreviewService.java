package come.back.gotoday.recommend.service;

import come.back.gotoday.recommend.dto.TourPlacePreviewRequest;
import come.back.gotoday.recommend.dto.TourPlacePreviewResponse;
import come.back.gotoday.tour.entity.Tour;
import come.back.gotoday.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourPlacePreviewService {

    private final TourRepository tourRepository;

    public List<TourPlacePreviewResponse> previewTourPlaces(
            TourPlacePreviewRequest request
    ) {
        String area = normalizeArea(request.getAreaOrDefault());
        int topK = request.getTopKOrDefault();

        List<Tour> tours = tourRepository.findRecommendedToursByArea(
                area,
                PageRequest.of(0, topK)
        );

        return tours.stream()
                .map(tour -> TourPlacePreviewResponse.from(
                        tour,
                        createRecommendationReason(area)
                ))
                .toList();
    }

    private String normalizeArea(String area) {
        if (area == null || area.isBlank()) {
            return "";
        }

        String trimmedArea = area.trim();

        if ("서울특별시".equals(trimmedArea)) {
            return "서울";
        }

        if (trimmedArea.startsWith("서울특별시 ")) {
            return trimmedArea.replaceFirst("^서울특별시\\s*", "").trim();
        }

        return trimmedArea;
    }

    private String createRecommendationReason(String area) {
        if (area == null || area.isBlank()) {
            return "관광지 데이터를 기반으로 추천되었습니다.";
        }

        return area + " 지역의 관광지 데이터를 기반으로 추천되었습니다.";
    }
}