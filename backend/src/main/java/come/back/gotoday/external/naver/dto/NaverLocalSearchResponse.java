package come.back.gotoday.external.naver.dto;

import java.util.List;

public record NaverLocalSearchResponse(
        String lastBuildDate,
        int total,
        int start,
        int display,
        List<NaverLocalItem> items
) {
}
