package come.back.gotoday.admin.service;

import come.back.gotoday.admin.dto.request.AdminPlaceCreateRequest;
import come.back.gotoday.admin.dto.request.AdminPlaceUpdateRequest;
import come.back.gotoday.admin.dto.response.AdminPlaceResponse;
import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPlaceService {

    private static final String ADMIN_SOURCE = "ADMIN";

    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public AdminPlaceResponse createPlace(AdminPlaceCreateRequest request) {
        log.info("관리자 장소 등록 시작: name={}, categoryId={}", request.name(), request.categoryId());

        validateDuplicatePlace(request.name(), request.address());

        Category category = getCategory(request.categoryId());

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
                ADMIN_SOURCE,
                request.externalId(),
                true
        );

        Place savedPlace = placeRepository.save(place);

        log.info("관리자 장소 등록 완료: placeId={}, name={}", savedPlace.getId(), savedPlace.getName());

        return AdminPlaceResponse.from(savedPlace);
    }

    @Transactional
    public AdminPlaceResponse updatePlace(Long placeId, AdminPlaceUpdateRequest request) {
        log.info("관리자 장소 수정 시작: placeId={}, name={}", placeId, request.name());

        Place place = getPlace(placeId);
        Category category = getCategory(request.categoryId());

        validateDuplicatePlaceForUpdate(request.name(), request.address(), placeId);

        place.update(
                category,
                request.name(),
                request.address(),
                request.roadAddress(),
                request.latitude(),
                request.longitude(),
                request.phone(),
                request.placeUrl(),
                request.description(),
                request.externalId()
        );

        log.info("관리자 장소 수정 완료: placeId={}", placeId);

        return AdminPlaceResponse.from(place);
    }

    @Transactional
    public void deletePlace(Long placeId) {
        log.info("관리자 장소 비활성화 시작: placeId={}", placeId);

        Place place = getPlace(placeId);
        place.deactivate();

        log.info("관리자 장소 비활성화 완료: placeId={}", placeId);
    }

    private Place getPlace(Long placeId) {
        return placeRepository.findByIdAndIsActiveTrue(placeId)
                .orElseThrow(() -> {
                    log.warn("관리자 장소 처리 실패: 존재하지 않거나 비활성화된 장소 placeId={}", placeId);
                    return new BusinessException(ErrorCode.PLACE_NOT_FOUND);
                });
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("관리자 장소 처리 실패: 존재하지 않는 카테고리 categoryId={}", categoryId);
                    return new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
                });
    }

    private void validateDuplicatePlace(String name, String address) {
        if (placeRepository.existsByNameAndAddressAndIsActiveTrue(name, address)) {
            log.warn("관리자 장소 등록 실패: 중복 장소 name={}, address={}", name, address);
            throw new BusinessException(ErrorCode.PLACE_ALREADY_EXISTS);
        }
    }

    private void validateDuplicatePlaceForUpdate(String name, String address, Long placeId) {
        if (placeRepository.existsByNameAndAddressAndIsActiveTrueAndIdNot(name, address, placeId)) {
            log.warn("관리자 장소 수정 실패: 중복 장소 name={}, address={}, placeId={}", name, address, placeId);
            throw new BusinessException(ErrorCode.PLACE_ALREADY_EXISTS);
        }
    }
}