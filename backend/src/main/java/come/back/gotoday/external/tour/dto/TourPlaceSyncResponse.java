package come.back.gotoday.external.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TourPlaceSyncResponse {

    private String areaCode;
    private int syncedCount;
}