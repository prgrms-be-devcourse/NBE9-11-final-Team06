package come.back.gotoday.admin.service;

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
    public int syncAllSeoulTours() {
        return tourSyncService.syncAllSeoulTours();
    }
}