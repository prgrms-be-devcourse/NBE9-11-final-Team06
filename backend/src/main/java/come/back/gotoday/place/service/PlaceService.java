package come.back.gotoday.place.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.external.naver.NaverLocalSearchClient;
import come.back.gotoday.external.naver.dto.NaverLocalSearchResponse;
import come.back.gotoday.place.dto.PlaceCreateRequest;
import come.back.gotoday.place.dto.PlaceResponse;
import come.back.gotoday.place.dto.PlaceSearchResponse;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;
    private final NaverLocalSearchClient naverLocalSearchClient;

    @Transactional
    public Long createPlace(PlaceCreateRequest request) {
        log.info("장소 생성 처리 시작: name={}, categoryId={}", request.name(), request.categoryId());

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> {
                    log.warn("장소 생성 실패: 존재하지 않는 카테고리입니다. categoryId={}", request.categoryId());
                    return new IllegalArgumentException("카테고리가 존재하지 않습니다.");
                });

        Place place = Place.create(
                category,
                request.name(),
                request.address(),
                request.roadAddress(),
                request.latitude(),
                request.longitude(),
                request.phone(),
                request.placeUrl(),
                request.description(),
                request.source(),
                request.externalId(),
                true // isActive 기본값
        );

        placeRepository.save(place);
        log.info("장소 생성 처리 완료: placeId={}, name={}", place.getId(), place.getName());

        return place.getId();
    }

    public PlaceResponse getPlace(Long placeId) {
        log.info("장소 단건 조회 처리 시작: placeId={}", placeId);

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> {
                    log.warn("장소 단건 조회 실패: 존재하지 않는 장소입니다. placeId={}", placeId);
                    return new IllegalArgumentException("장소가 존재하지 않습니다.");
                });

        PlaceResponse response = new PlaceResponse(
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCategory().getId()
        );

        log.info("장소 단건 조회 처리 완료: placeId={}", placeId);
        return response;
    }

    public List<PlaceResponse> getPlaces() {
        log.info("장소 목록 조회 처리 시작");

        List<PlaceResponse> places = placeRepository.findAll()
                .stream()
                .map(place -> new PlaceResponse(
                        place.getId(),
                        place.getName(),
                        place.getAddress(),
                        place.getLatitude(),
                        place.getLongitude(),
                        place.getCategory().getId()
                ))
                .toList();

        log.info("장소 목록 조회 처리 완료: resultCount={}", places.size());
        return places;
    }

    public List<PlaceSearchResponse> searchPlaces(String query) {
        log.info("장소 검색 처리 시작: query={}", query);

        if (query == null || query.isBlank()) {
            log.warn("장소 검색 실패: 검색어가 비어 있습니다.");
            return List.of();
        }

        NaverLocalSearchResponse response = naverLocalSearchClient.search(query, 5, 1);
        if (response == null || response.items() == null) {
            log.warn("장소 검색 실패: 네이버 지역 검색 API 응답이 비어 있습니다. query={}", query);
            return List.of();
        }

        List<PlaceSearchResponse> places = response.items()
                .stream()
                .map(PlaceSearchResponse::from)
                .toList();

        log.info("장소 검색 처리 완료: query={}, resultCount={}", query, places.size());
        return places;
    }

    @Transactional
    public void deletePlace(Long placeId) {
        log.info("장소 삭제 처리 시작: placeId={}", placeId);

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> {
                    log.warn("장소 삭제 실패: 존재하지 않는 장소입니다. placeId={}", placeId);
                    return new IllegalArgumentException("장소가 존재하지 않습니다.");
                });

        placeRepository.delete(place);
        log.info("장소 삭제 처리 완료: placeId={}", placeId);
    }
}
