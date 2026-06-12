package come.back.gotoday.place.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.place.dto.PlaceCreateRequest;
import come.back.gotoday.place.dto.PlaceResponse;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Long createPlace(PlaceCreateRequest request) {

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다."));

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

        return place.getId();
    }

    public PlaceResponse getPlace(Long placeId) {

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("장소가 존재하지 않습니다."));

        return new PlaceResponse(
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCategory().getId()
        );
    }

    public List<PlaceResponse> getPlaces() {
        return placeRepository.findAll()
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
    }

    @Transactional
    public void deletePlace(Long placeId) {

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("장소가 존재하지 않습니다."));

        placeRepository.delete(place);
    }
}
