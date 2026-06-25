package come.back.gotoday.external.tour.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiBody(
        TourApiItems items,
        Integer numOfRows,
        Integer pageNo,
        Integer totalCount
) {

    public List<TourApiItem> getItemsOrEmpty() {
        if (items == null) {
            return new ArrayList<>();
        }

        return items.getItemsOrEmpty();
    }
}