package come.back.gotoday.place;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.external.kakao.dto.KakaoPlaceDocument;
import come.back.gotoday.external.naver.NaverLocalSearchClient;
import come.back.gotoday.external.naver.NaverReverseGeocodingClient;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.dto.ReverseGeocodingResponse;
import come.back.gotoday.place.repository.PlaceRepository;
import come.back.gotoday.place.service.PlaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private NaverLocalSearchClient naverLocalSearchClient;

    @Mock
    private NaverReverseGeocodingClient naverReverseGeocodingClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private PlaceService placeService;

    @Test
    void 동일한_카카오_장소가_동시에_생성되어_중복키_예외가_발생하면_기존_장소를_반환한다() {
        // given
        KakaoPlaceDocument document = new KakaoPlaceDocument(
                "테스트 카페",
                "서울 종로구 테스트동 1",
                "서울 종로구 테스트로 1",
                "02-0000-0001",
                "http://place.map.kakao.com/mock-cafe-1",
                "음식점 > 카페",
                "0",
                "126.9573421635",
                "37.5746723659"
        );
        Category category = mock(Category.class);
        Place existingPlace = mock(Place.class);

        when(placeRepository.findBySourceAndExternalId("KAKAO", "mock-cafe-1"))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(existingPlace));
        when(placeRepository.saveAndFlush(any(Place.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        }).when(transactionTemplate).execute(any(TransactionCallback.class));

        // when
        Place result = placeService.getOrCreatePlace(document, category);

        // then
        assertThat(result).isSameAs(existingPlace);
        verify(placeRepository, times(3))
                .findBySourceAndExternalId("KAKAO", "mock-cafe-1");
        verify(placeRepository).saveAndFlush(any(Place.class));
        verify(transactionTemplate, times(2)).execute(any(TransactionCallback.class));
    }

    @Test
    void 네이버_지역_검색_API_timeout_발생시_빈_목록을_반환한다() {
        // given
        String query = "성수 카페";
        when(naverLocalSearchClient.search(query, 5, 1))
                .thenThrow(new BusinessException(ErrorCode.EXTERNAL_API_ERROR));

        // when
        List<?> result = placeService.searchPlaces(query);

        // then
        assertThat(result).isEmpty();
        verify(naverLocalSearchClient).search(query, 5, 1);
        verifyNoInteractions(placeRepository, categoryRepository, naverReverseGeocodingClient, transactionTemplate);
    }

    @Test
    void 네이버_역지오코딩_API_timeout_발생시_빈_지역_정보를_반환한다() {
        // given
        double latitude = 37.5665;
        double longitude = 126.9780;

        when(naverReverseGeocodingClient.reverseGeocode(latitude, longitude))
                .thenThrow(new BusinessException(ErrorCode.EXTERNAL_API_ERROR));

        // when
        ReverseGeocodingResponse result = placeService.reverseGeocode(latitude, longitude);

        // then
        assertThat(result.areaName()).isNull();
        assertThat(result.district()).isNull();
        assertThat(result.neighborhood()).isNull();
        verify(naverReverseGeocodingClient).reverseGeocode(latitude, longitude);
        verifyNoInteractions(placeRepository, categoryRepository, naverLocalSearchClient, transactionTemplate);
    }
}
