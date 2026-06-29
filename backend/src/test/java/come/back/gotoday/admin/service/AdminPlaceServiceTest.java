package come.back.gotoday.admin.service;

import come.back.gotoday.admin.dto.request.AdminPlaceCreateRequest;
import come.back.gotoday.admin.dto.request.AdminPlaceUpdateRequest;
import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPlaceServiceTest {

    @InjectMocks
    private AdminPlaceService adminPlaceService;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("관리자는 장소를 등록할 수 있다")
    void createPlace_success() {
        AdminPlaceCreateRequest request = createRequest();
        Category category = mock(Category.class);

        when(category.getId()).thenReturn(1L);

        when(placeRepository.existsByNameAndAddressAndIsActiveTrue(request.name(), request.address()))
                .thenReturn(false);
        when(categoryRepository.findById(request.categoryId()))
                .thenReturn(Optional.of(category));
        when(placeRepository.save(any(Place.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminPlaceService.createPlace(request);

        verify(placeRepository).save(any(Place.class));
    }

    @Test
    @DisplayName("중복된 장소를 등록하면 예외가 발생한다")
    void createPlace_duplicate_fail() {
        AdminPlaceCreateRequest request = createRequest();

        when(placeRepository.existsByNameAndAddressAndIsActiveTrue(request.name(), request.address()))
                .thenReturn(true);

        assertThatThrownBy(() -> adminPlaceService.createPlace(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PLACE_ALREADY_EXISTS);

        verify(categoryRepository, never()).findById(any());
        verify(placeRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 장소를 등록하면 예외가 발생한다")
    void createPlace_categoryNotFound_fail() {
        AdminPlaceCreateRequest request = createRequest();

        when(placeRepository.existsByNameAndAddressAndIsActiveTrue(request.name(), request.address()))
                .thenReturn(false);
        when(categoryRepository.findById(request.categoryId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPlaceService.createPlace(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

        verify(placeRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 장소를 수정하면 예외가 발생한다")
    void updatePlace_placeNotFound_fail() {
        Long placeId = 999L;
        AdminPlaceUpdateRequest request = updateRequest();

        when(placeRepository.findByIdAndIsActiveTrue(placeId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPlaceService.updatePlace(placeId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PLACE_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 장소를 수정하면 예외가 발생한다")
    void updatePlace_categoryNotFound_fail() {
        Long placeId = 1L;
        AdminPlaceUpdateRequest request = updateRequest();
        Place place = mock(Place.class);

        when(placeRepository.findByIdAndIsActiveTrue(placeId))
                .thenReturn(Optional.of(place));
        when(categoryRepository.findById(request.categoryId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPlaceService.updatePlace(placeId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("중복된 장소 정보로 수정하면 예외가 발생한다")
    void updatePlace_duplicate_fail() {
        Long placeId = 1L;
        AdminPlaceUpdateRequest request = updateRequest();
        Place place = mock(Place.class);
        Category category = mock(Category.class);

        when(placeRepository.findByIdAndIsActiveTrue(placeId))
                .thenReturn(Optional.of(place));
        when(categoryRepository.findById(request.categoryId()))
                .thenReturn(Optional.of(category));
        when(placeRepository.existsByNameAndAddressAndIsActiveTrueAndIdNot(
                request.name(),
                request.address(),
                placeId
        )).thenReturn(true);

        assertThatThrownBy(() -> adminPlaceService.updatePlace(placeId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PLACE_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("존재하지 않는 장소를 삭제하면 예외가 발생한다")
    void deletePlace_placeNotFound_fail() {
        Long placeId = 999L;

        when(placeRepository.findByIdAndIsActiveTrue(placeId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPlaceService.deletePlace(placeId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PLACE_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자는 장소를 삭제 처리할 수 있다")
    void deletePlace_success() {
        Long placeId = 1L;
        Place place = mock(Place.class);

        when(placeRepository.findByIdAndIsActiveTrue(placeId))
                .thenReturn(Optional.of(place));

        adminPlaceService.deletePlace(placeId);

        verify(place).deactivate();
    }

    private AdminPlaceCreateRequest createRequest() {
        return new AdminPlaceCreateRequest(
                1L,
                "테스트 장소",
                "서울특별시 강남구 테스트로 1",
                "서울특별시 강남구 테스트로 1",
                BigDecimal.valueOf(37.5665),
                BigDecimal.valueOf(126.9780),
                "02-1234-5678",
                "https://example.com",
                "테스트 장소입니다.",
                "external-test-001"
        );
    }

    private AdminPlaceUpdateRequest updateRequest() {
        return new AdminPlaceUpdateRequest(
                1L,
                "수정 테스트 장소",
                "서울특별시 강남구 수정로 1",
                "서울특별시 강남구 수정로 1",
                BigDecimal.valueOf(37.5666),
                BigDecimal.valueOf(126.9781),
                "02-9999-9999",
                "https://example.com/updated",
                "수정 테스트 장소입니다.",
                "external-test-001"
        );
    }
}