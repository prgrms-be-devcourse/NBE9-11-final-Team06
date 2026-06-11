package come.back.gotoday.external.seoul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public record SeoulEventResponse(
        @JsonProperty("culturalEventInfo") CulturalEventInfo culturalEventInfo
) {

    public record CulturalEventInfo(
                @JsonProperty("list_total_count") int listTotalCount,
            @JsonProperty("row") List<EventRow> row
    ) {}

    public record EventRow(
            @JsonProperty("TITLE") String title,
            @JsonProperty("CODENAME") String codeName,
            @JsonProperty("STRTDATE") String startDate,
            @JsonProperty("END_DATE") String endDate,
            @JsonProperty("GUNAME") String guName,
            @JsonProperty("PLACE") String placeName,
            @JsonProperty("ORG_LINK") String orgLink,
            @JsonProperty("MAIN_IMG") String mainImg,
            @JsonProperty("USE_TRGT") String useTrgt,
            @JsonProperty("USE_FEE") String useFee,
            @JsonProperty("LAT") Double lat,
            @JsonProperty("LOT") Double lot
    ) {
        /**
         * 제목과 시작일을 조합하여 고유한 external_id를 생성합니다.
         * 공백을 제거한 [제목] + [시작일의 날짜부분] 구조로 만들어 유일성을 보장합니다.
         * 예: "마티네콘서트#3_2026-10-15"
         */
        public String externalId() {
            String cleanTitle = (this.title != null) ? this.title.replaceAll("\\s+", "") : "";
            // "2026-10-15 00:00:00.0" 에서 날짜인 "2026-10-15"만 추출
            String cleanDate = (this.startDate != null) ? this.startDate.split(" ")[0] : "";

            return cleanTitle + "_" + cleanDate;
        }

    }
    public static SeoulEventResponse empty() {
        // 내부 하위 DTO들도 빈 객체나 빈 리스트를 가지도록 생성하여 반환합니다.
        return new SeoulEventResponse(new CulturalEventInfo(0, Collections.emptyList()));
    }
}