package come.back.gotoday.admin.service;

import come.back.gotoday.admin.dto.request.AdminKakaoPlaceSyncRequest;
import come.back.gotoday.admin.dto.response.AdminSyncResponse;
import come.back.gotoday.event.service.EventBatchService;
import come.back.gotoday.tour.service.TourSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminSyncServiceTest {

    @Mock
    private EventBatchService eventBatchService;

    @Mock
    private TourSyncService tourSyncService;

    @Mock
    private KakaoPlaceSyncService kakaoPlaceSyncService;

    @InjectMocks
    private AdminSyncService adminSyncService;

    @Test
    @DisplayName("행사 동기화를 실행하고 COMPLETED 응답을 반환한다")
    void syncEventsReturnsCompletedResponse() {
        AdminSyncResponse response = adminSyncService.syncEvents();

        assertThat(response.target()).isEqualTo("EVENT");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.processedCount()).isZero();
        assertThat(response.message()).isEqualTo("서울시 행사 동기화가 완료되었습니다. 처리 건수는 배치 로그에서 확인해주세요.");

        then(eventBatchService).should().syncSeoulEvents();
    }

    @Test
    @DisplayName("관광지 동기화를 실행하고 처리 건수를 응답한다")
    void syncTourPlacesReturnsProcessedCount() {
        given(tourSyncService.syncTours("1", null)).willReturn(10);

        AdminSyncResponse response = adminSyncService.syncTourPlaces("1");

        assertThat(response.target()).isEqualTo("TOUR");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.processedCount()).isEqualTo(10);
        assertThat(response.message()).isEqualTo("관광공사 관광지 동기화가 완료되었습니다.");

        then(tourSyncService).should().syncTours("1", null);
    }

    @Test
    @DisplayName("카카오 장소 DB 기준 동기화를 실행하고 처리 건수를 응답한다")
    void syncKakaoPlacesReturnsProcessedCount() {
        given(kakaoPlaceSyncService.syncKakaoPlacesFromBasePlaces(30)).willReturn(15);

        AdminSyncResponse response = adminSyncService.syncKakaoPlaces(30);

        assertThat(response.target()).isEqualTo("KAKAO_PLACE");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.processedCount()).isEqualTo(15);
        assertThat(response.message()).isEqualTo("카카오 장소 DB 기준 동기화가 완료되었습니다.");

        then(kakaoPlaceSyncService).should().syncKakaoPlacesFromBasePlaces(30);
    }

    @Test
    @DisplayName("카카오 장소 좌표 기반 동기화를 실행하고 처리 건수를 응답한다")
    void syncKakaoPlacesNearbyReturnsProcessedCount() {
        AdminKakaoPlaceSyncRequest request = org.mockito.Mockito.mock(AdminKakaoPlaceSyncRequest.class);

        given(kakaoPlaceSyncService.syncKakaoPlacesNearby(request)).willReturn(8);

        AdminSyncResponse response = adminSyncService.syncKakaoPlacesNearby(request);

        assertThat(response.target()).isEqualTo("KAKAO_PLACE_NEARBY");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.processedCount()).isEqualTo(8);
        assertThat(response.message()).isEqualTo("카카오 장소 좌표 기반 동기화가 완료되었습니다.");

        then(kakaoPlaceSyncService).should().syncKakaoPlacesNearby(request);
    }

    @Test
    @DisplayName("서울 전체 관광지 동기화 처리 건수를 반환한다")
    void syncAllSeoulToursReturnsProcessedCount() {
        given(tourSyncService.syncAllSeoulTours()).willReturn(462);

        int processedCount = adminSyncService.syncAllSeoulTours();

        assertThat(processedCount).isEqualTo(462);

        then(tourSyncService).should().syncAllSeoulTours();
    }
}