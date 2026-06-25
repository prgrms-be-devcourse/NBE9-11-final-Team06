package come.back.gotoday.tour.controller;

import come.back.gotoday.tour.service.TourSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tours")
public class TourSyncController {

    private final TourSyncService tourSyncService;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncAllSeoulTours() {
        int syncedCount = tourSyncService.syncAllSeoulTours();

        return ResponseEntity.ok(Map.of(
                "message", "서울 관광지 동기화가 완료되었습니다.",
                "syncedCount", syncedCount
        ));
    }

    @PostMapping("/sync/area")
    public ResponseEntity<Map<String, Object>> syncToursByArea(
            @RequestParam String areaCode,
            @RequestParam String sigunguCode
    ) {
        int syncedCount = tourSyncService.syncTours(areaCode, sigunguCode);

        return ResponseEntity.ok(Map.of(
                "message", "관광지 동기화가 완료되었습니다.",
                "areaCode", areaCode,
                "sigunguCode", sigunguCode,
                "syncedCount", syncedCount
        ));
    }
}