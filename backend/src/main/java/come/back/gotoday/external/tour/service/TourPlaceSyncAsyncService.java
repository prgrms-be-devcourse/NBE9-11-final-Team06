package come.back.gotoday.external.tour.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourPlaceSyncAsyncService {

    private final TourPlaceSyncService tourPlaceSyncService;

    @Async("tourPlaceSyncExecutor")
    public void syncTourPlacesAsync(String areaCode) {
        try {
            log.info("관광공사 관광지 비동기 동기화 시작: areaCode={}", areaCode);

            int syncedCount = tourPlaceSyncService.syncTourPlaces(areaCode);

            log.info(
                    "관광공사 관광지 비동기 동기화 완료: areaCode={}, syncedCount={}",
                    areaCode,
                    syncedCount
            );
        } catch (Exception e) {
            log.warn(
                    "관광공사 관광지 비동기 동기화 실패: areaCode={}, reason={}",
                    areaCode,
                    e.getMessage(),
                    e
            );
        }
    }
}