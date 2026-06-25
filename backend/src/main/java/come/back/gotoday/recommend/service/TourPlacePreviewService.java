package come.back.gotoday.recommend.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import come.back.gotoday.recommend.dto.TourPlacePreviewRequest;
import come.back.gotoday.recommend.dto.TourPlacePreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourPlacePreviewService {

    private final PlaceRepository placeRepository;

    public List<TourPlacePreviewResponse> previewTourPlaces(
            TourPlacePreviewRequest request
    ) {
        List<Place> tourPlaces = placeRepository.findActivePlacesBySourceAndArea(
                        Place.TOUR_API_SOURCE,
                        normalizeArea(request.getAreaOrDefault())
                )
                .stream()
                .filter(place -> place.getLatitude() != null && place.getLongitude() != null)
                .limit(request.getTopKOrDefault())
                .toList();

        if (tourPlaces.isEmpty()) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }

        return tourPlaces.stream()
                .map(place -> new TourPlacePreviewResponse(
                        place.getId(),
                        place.getName(),
                        place.getCategory() != null ? place.getCategory().getName() : "관광지",
                        place.getAddress(),
                        place.getLatitude(),
                        place.getLongitude(),
                        place.getPlaceUrl(),
                        "선택한 지역의 관광지 데이터를 기반으로 추천되었습니다."
                ))
                .toList();
    }

    private String normalizeArea(String area) {
        if (area == null || area.isBlank()) {
            return null;
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
}