package come.back.gotoday.external.tour.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiResponseWrapper(
        TourApiResponse response
) {

    public List<TourApiItem> getItemsOrEmpty() {
        if (response == null || response.body() == null) {
            return new ArrayList<>();
        }

        return response.body().getItemsOrEmpty();
    }

    public Integer getTotalCountOrZero() {
        if (response == null || response.body() == null || response.body().totalCount() == null) {
            return 0;
        }

        return response.body().totalCount();
    }
}