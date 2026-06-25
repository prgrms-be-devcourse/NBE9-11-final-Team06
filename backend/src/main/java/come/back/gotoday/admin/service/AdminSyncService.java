package come.back.gotoday.admin.service;

import come.back.gotoday.admin.dto.request.AdminKakaoPlaceSyncRequest;
import come.back.gotoday.admin.dto.response.AdminSyncResponse;
import come.back.gotoday.event.service.EventBatchService;
import come.back.gotoday.external.tour.service.TourPlaceSyncAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminSyncService {

    private final EventBatchService eventBatchService;
    private final TourPlaceSyncAsyncService tourPlaceSyncAsyncService;
    private final KakaoPlaceSyncService kakaoPlaceSyncService;

    public AdminSyncResponse syncEvents() {
        eventBatchService.syncSeoulEvents();

        return new AdminSyncResponse(
                "SEOUL_EVENT",
                "SUCCESS",
                null,
                "서울시 행사 동기화가 완료되었습니다."
        );
    }

    public AdminSyncResponse syncTourPlaces(String areaCode) {
        tourPlaceSyncAsyncService.syncTourPlacesAsync(areaCode);

        return new AdminSyncResponse(
                "TOUR_PLACE",
                "ACCEPTED",
                null,
                "관광공사 관광지 동기화 요청이 접수되었습니다."
        );
    }

    public AdminSyncResponse syncKakaoPlaces(int limit) {
        int processedCount = kakaoPlaceSyncService.syncKakaoPlacesFromBasePlaces(limit);

        return new AdminSyncResponse(
                "KAKAO_PLACE",
                "SUCCESS",
                processedCount,
                "DB 기준 장소 주변 카카오 장소 동기화가 완료되었습니다."
        );
    }

    public AdminSyncResponse syncKakaoPlacesNearby(AdminKakaoPlaceSyncRequest request) {
        int processedCount = kakaoPlaceSyncService.syncKakaoPlacesNearby(request);

        return new AdminSyncResponse(
                "KAKAO_PLACE_NEARBY",
                "SUCCESS",
                processedCount,
                "카카오 장소 좌표 기반 동기화 요청이 처리되었습니다."
        );
    }
}