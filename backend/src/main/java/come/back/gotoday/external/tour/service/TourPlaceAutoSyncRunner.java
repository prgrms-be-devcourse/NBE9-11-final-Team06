package come.back.gotoday.external.tour.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "external.tour.auto-sync",
        name = "enabled",
        havingValue = "true"
)
public class TourPlaceAutoSyncRunner {

    private final TourPlaceSyncAsyncService tourPlaceSyncAsyncService;

    @Value("${external.tour.auto-sync.area-code:1}")
    private String areaCode;

    @EventListener(ApplicationReadyEvent.class)
    public void syncTourPlacesOnApplicationReady() {
        log.info("관광공사 관광지 자동 동기화 요청 접수: areaCode={}", areaCode);

        tourPlaceSyncAsyncService.syncTourPlacesAsync(areaCode);
    }
}