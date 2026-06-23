package come.back.gotoday.external.tour.controller;

import come.back.gotoday.external.tour.dto.TourPlaceSyncResponse;
import come.back.gotoday.external.tour.service.TourPlaceSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tour-places")
public class TourPlaceAdminController {

    private final TourPlaceSyncService tourPlaceSyncService;

    @PostMapping("/sync")
    public TourPlaceSyncResponse syncTourPlaces(
            @RequestParam(defaultValue = "1") String areaCode
    ) {
        int syncedCount = tourPlaceSyncService.syncTourPlaces(areaCode);

        return new TourPlaceSyncResponse(areaCode, syncedCount);
    }
}