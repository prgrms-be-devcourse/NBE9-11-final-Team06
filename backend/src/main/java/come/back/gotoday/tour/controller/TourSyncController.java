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
        tourSyncService.syncAllSeoulToursAsync();

        return ResponseEntity.accepted().body(Map.of(
                "message", "서울 관광지 동기화 작업이 시작되었습니다."
        ));
    }

    @PostMapping("/sync/area")
    public ResponseEntity<Map<String, Object>> syncToursByArea(
            @RequestParam String areaCode,
            @RequestParam String sigunguCode
    ) {
        tourSyncService.syncToursAsync(areaCode, sigunguCode);

        return ResponseEntity.accepted().body(Map.of(
                "message", "관광지 동기화 작업이 시작되었습니다.",
                "areaCode", areaCode,
                "sigunguCode", sigunguCode
        ));
    }
}