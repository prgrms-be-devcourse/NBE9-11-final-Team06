package come.back.gotoday.external.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TourPlaceSyncAcceptedResponse {

    private String areaCode;
    private String status;
    private String message;
}