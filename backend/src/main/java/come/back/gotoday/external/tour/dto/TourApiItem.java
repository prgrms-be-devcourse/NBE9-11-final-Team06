package come.back.gotoday.external.tour.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiItem(
        String contentid,
        String contenttypeid,
        String title,
        String addr1,
        String addr2,
        String zipcode,
        String tel,
        String firstimage,
        String firstimage2,
        String areacode,
        String sigungucode,
        String cat1,
        String cat2,
        String cat3,
        String mapx,
        String mapy,
        String createdtime,
        String modifiedtime
) {
}