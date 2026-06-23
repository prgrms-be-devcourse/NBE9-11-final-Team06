package come.back.gotoday.external.tour.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiResponse {

    private TourResponse response;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TourResponse {

        private TourHeader header;
        private TourBody body;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TourHeader {

        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TourBody {

        private TourItems items;
        private int numOfRows;
        private int pageNo;
        private int totalCount;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TourItems {

        private List<TourPlaceItem> item;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TourPlaceItem {

        @JsonProperty("contentid")
        private String contentId;

        @JsonProperty("contenttypeid")
        private String contentTypeId;

        private String title;

        private String addr1;

        private String addr2;

        @JsonProperty("mapx")
        private BigDecimal longitude;

        @JsonProperty("mapy")
        private BigDecimal latitude;

        private String tel;

        private String firstimage;

        private String firstimage2;

        @JsonProperty("areacode")
        private String areaCode;

        @JsonProperty("sigungucode")
        private String sigunguCode;
    }
}