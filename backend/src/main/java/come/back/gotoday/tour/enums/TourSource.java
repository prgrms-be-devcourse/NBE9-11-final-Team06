package come.back.gotoday.tour.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TourSource {

    TOUR_API("TOUR_API");

    private final String code;
}