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

    private final TourSyncService tourSyncService;

    @Transactional
    public AdminSyncResponse syncEvents() {
        return null;
    }

    @Transactional
    public AdminSyncResponse syncTourPlaces(String areaCode) {
        tourSyncService.syncTours(areaCode, null);
        return null;
    }

    @Transactional
    public AdminSyncResponse syncKakaoPlaces(int limit) {
        return null;
    }

    @Transactional
    public AdminSyncResponse syncKakaoPlacesNearby(AdminKakaoPlaceSyncRequest request) {
        return null;
    }

    @Transactional
    public int syncAllSeoulTours() {
        return tourSyncService.syncAllSeoulTours();
    }
}