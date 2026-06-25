package come.back.gotoday.external.tour.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiHeader(
        String resultCode,
        String resultMsg
) {
}