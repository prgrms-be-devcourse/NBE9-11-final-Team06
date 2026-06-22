
package come.back.gotoday.weather.service;

import come.back.gotoday.event.entity.Event;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 행사 카테고리와 제목을 기준으로 실내·실외 여부를 판단하는 정책입니다.
 *
 * true는 실내, false는 실외, null은 판단 불가를 의미합니다.
 * 추후 Event에 실내·실외 속성이 추가되면 해당 값을 우선 사용하도록 확장할 수 있습니다.
 */
@Component
public class EventIndoorOutdoorPolicy {

    private static final List<String> INDOOR_KEYWORDS = List.of(
            "전시", "미술관", "박물관", "공연", "콘서트", "연극", "뮤지컬",
            "영화", "상영", "실내", "갤러리", "체험관", "과학관", "도서관"
    );

    private static final List<String> OUTDOOR_KEYWORDS = List.of(
            "축제", "공원", "야외", "광장", "산책", "플리마켓", "마켓",
            "불꽃", "퍼레이드", "걷기", "러닝", "등산", "캠핑"
    );

    /**
     * 행사 실내·실외 여부를 판단합니다.
     *
     * @param event 대상 행사
     * @return 실내면 true, 실외면 false, 판단할 수 없으면 null
     */
    public Boolean isIndoor(Event event) {
        if (event == null) {
            return null;
        }

        String searchableText = buildSearchableText(event);

        if (containsKeyword(searchableText, INDOOR_KEYWORDS)) {
            return true;
        }

        if (containsKeyword(searchableText, OUTDOOR_KEYWORDS)) {
            return false;
        }

        return null;
    }

    private String buildSearchableText(Event event) {
        String categoryName = event.getCategory() != null
                ? event.getCategory().getName()
                : "";
        String title = event.getTitle() != null ? event.getTitle() : "";
        String description = event.getDescription() != null ? event.getDescription() : "";

        return (categoryName + " " + title + " " + description)
                .toLowerCase(Locale.ROOT);
    }

    private boolean containsKeyword(String searchableText, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(searchableText::contains);
    }
}
