package come.back.gotoday.admin.service;

import come.back.gotoday.admin.dto.request.AdminKakaoPlaceSyncRequest;
import come.back.gotoday.admin.dto.response.AdminSyncResponse;
import come.back.gotoday.tour.service.TourSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSyncService {

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_SKIPPED = "SKIPPED";

    private final TourSyncService tourSyncService;
    private final KakaoPlaceSyncService kakaoPlaceSyncService;

    @Transactional
    public AdminSyncResponse syncEvents() {
        return new AdminSyncResponse(
                "EVENT",
                STATUS_SKIPPED,
                0,
                "서울시 행사 동기화 서비스가 현재 연결되어 있지 않습니다."
        );
    }

    @Transactional
    public AdminSyncResponse syncTourPlaces(String areaCode) {
        int processedCount = tourSyncService.syncTours(areaCode, null);

        return new AdminSyncResponse(
                "TOUR",
                STATUS_COMPLETED,
                processedCount,
                "관광공사 관광지 동기화가 완료되었습니다."
        );
    }

    @Transactional
    public AdminSyncResponse syncKakaoPlaces(int limit) {
        int processedCount = kakaoPlaceSyncService.syncKakaoPlacesFromBasePlaces(limit);

        return new AdminSyncResponse(
                "KAKAO_PLACE",
                STATUS_COMPLETED,
                processedCount,
                "카카오 장소 DB 기준 동기화가 완료되었습니다."
        );
    }

    @Transactional
    public AdminSyncResponse syncKakaoPlacesNearby(AdminKakaoPlaceSyncRequest request) {
        int processedCount = kakaoPlaceSyncService.syncKakaoPlacesNearby(request);

        return new AdminSyncResponse(
                "KAKAO_PLACE_NEARBY",
                STATUS_COMPLETED,
                processedCount,
                "카카오 장소 좌표 기반 동기화가 완료되었습니다."
        );
    }

    @Transactional
    public int syncAllSeoulTours() {
        return tourSyncService.syncAllSeoulTours();
    }
}