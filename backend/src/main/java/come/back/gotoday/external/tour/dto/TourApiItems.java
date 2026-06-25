package come.back.gotoday.external.tour.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiItems(
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        List<TourApiItem> item
) {

    public List<TourApiItem> getItemsOrEmpty() {
        if (item == null) {
            return new ArrayList<>();
        }

        return item;
    }
}