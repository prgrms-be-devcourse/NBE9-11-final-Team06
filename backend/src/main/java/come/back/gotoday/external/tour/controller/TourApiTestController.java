package come.back.gotoday.external.tour.controller;

import come.back.gotoday.external.tour.TourApiClient;
import come.back.gotoday.external.tour.dto.TourApiResponse.TourPlaceItem;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TourApiTestController {

    private final TourApiClient tourApiClient;

    @GetMapping("/api/test/tour-places")
    public List<TourPlaceItem> getTourPlaces(
            @RequestParam(defaultValue = "1") String areaCode,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "5") int numOfRows
    ) {
        return tourApiClient.fetchTourPlaces(areaCode, pageNo, numOfRows);
    }
}