package come.back.gotoday.weather.service;

import come.back.gotoday.event.entity.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("행사 실내·실외 판단 정책 단위 테스트")
class EventIndoorOutdoorPolicyTest {

    private final EventIndoorOutdoorPolicy eventIndoorOutdoorPolicy = new EventIndoorOutdoorPolicy();

    @Test
    @DisplayName("행사가 없으면 판단 불가를 반환한다")
    void isIndoorReturnsNullWhenEventIsNull() {
        Boolean result = eventIndoorOutdoorPolicy.isIndoor(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("행사 제목에 실내 키워드가 있으면 true를 반환한다")
    void isIndoorReturnsTrueWhenTitleContainsIndoorKeyword() {
        Event event = mockEvent("국립현대미술관 특별 전시", null);

        Boolean result = eventIndoorOutdoorPolicy.isIndoor(event);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("행사 설명에 실내 키워드가 있으면 true를 반환한다")
    void isIndoorReturnsTrueWhenDescriptionContainsIndoorKeyword() {
        Event event = mockEvent("주말 문화 프로그램", "실내 체험관에서 진행됩니다.");

        Boolean result = eventIndoorOutdoorPolicy.isIndoor(event);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("행사 제목에 실외 키워드가 있으면 false를 반환한다")
    void isIndoorReturnsFalseWhenTitleContainsOutdoorKeyword() {
        Event event = mockEvent("한강공원 야외 축제", null);

        Boolean result = eventIndoorOutdoorPolicy.isIndoor(event);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("행사 설명에 실외 키워드가 있으면 false를 반환한다")
    void isIndoorReturnsFalseWhenDescriptionContainsOutdoorKeyword() {
        Event event = mockEvent("주말 문화 프로그램", "광장에서 진행하는 플리마켓입니다.");

        Boolean result = eventIndoorOutdoorPolicy.isIndoor(event);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("실내와 실외 키워드가 함께 있으면 실내 키워드를 우선한다")
    void isIndoorPrioritizesIndoorKeywordOverOutdoorKeyword() {
        Event event = mockEvent("실내 전시와 야외 산책 연계 프로그램", null);

        Boolean result = eventIndoorOutdoorPolicy.isIndoor(event);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("판단 키워드가 없으면 판단 불가를 반환한다")
    void isIndoorReturnsNullWhenNoKeywordExists() {
        Event event = mockEvent("문화 행사", "지역 주민을 위한 프로그램입니다.");

        Boolean result = eventIndoorOutdoorPolicy.isIndoor(event);

        assertThat(result).isNull();
    }

    private Event mockEvent(String title, String description) {
        Event event = mock(Event.class);
        when(event.getTitle()).thenReturn(title);
        when(event.getDescription()).thenReturn(description);
        return event;
    }
}
