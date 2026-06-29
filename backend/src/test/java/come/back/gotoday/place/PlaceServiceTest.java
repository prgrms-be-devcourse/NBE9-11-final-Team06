
package come.back.gotoday.place;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.external.kakao.dto.KakaoPlaceDocument;
import come.back.gotoday.external.naver.NaverLocalSearchClient;
import come.back.gotoday.external.naver.NaverReverseGeocodingClient;
import come.back.gotoday.place.entity.Place;
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
}
