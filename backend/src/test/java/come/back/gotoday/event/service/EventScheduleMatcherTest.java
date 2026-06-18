package come.back.gotoday.event.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EventScheduleMatcherTest {

    private final EventScheduleMatcher matcher = new EventScheduleMatcher();

    // -------------------------------------------------------------------------
    // 1. 프리패스 패턴 검증 (단순 시간, 오타, 상시 안내) -> 어떤 일정이든 true 여야 함
    // -------------------------------------------------------------------------
    @ParameterizedTest(name = " 텍스트: [{0}]")
    @MethodSource("provideFreePassEventTimes")
    @DisplayName("[프리패스 검증] 단순 시간/오타/상시 문구는 요일 필터링을 안전하게 통과해야 한다")
    void shouldAlwaysPass_WhenFreePassPatterns(String eventTime) {
        // Given: 유저가 2026년 7월 2일 (목요일) 단 하루 여행한다고 가정
        LocalDate start = LocalDate.of(2026, 7, 2);
        LocalDate end = LocalDate.of(2026, 7, 2);
        Set<DayOfWeek> userDays = Set.of(DayOfWeek.THURSDAY);

        // When
        boolean result = matcher.isEventAvailableOnDays(eventTime, start, end, userDays);

        // Then
        assertThat(result).isTrue();
    }

    private static Stream<String> provideFreePassEventTimes() {
        return Stream.of(
                "19:30", "11:00", "10:00 ~ 18:00 *입장마감: 전시 관람 종료 1시간 전",
                "17:00 ~ 18:20", "12:00 ~ 21:00", "11:00 ~ 12:00", "19:3~ 21:00", // 오타 포함
                "상세정보 시간내용 참조", "홈페이지 참조", "프로그램별 상이", "세부 프로그램 상이(홈페이지 참고)",
                "프로그램 별 상이", "홈페이지 참고", "오전 10:00~11:30, 오후 1:00~2:30 / 3:00~4:30"
        );
    }

    // -------------------------------------------------------------------------
    // 2. 요일 및 날짜 조건부 매칭/탈락 정밀 검증
    // -------------------------------------------------------------------------
    @ParameterizedTest(name = " 결과 예상: {0} | 텍스트: [{1}]")
    @MethodSource("provideConditionalEventTimes")
    @DisplayName(" [조건부 매칭 검증] 요일, 날짜, 휴관일 조건이 유저의 여행 일정과 맞는지 정밀 판별한다")
    void shouldEvaluateCorrectly_WhenConditionalPatterns(boolean expected, String eventTime, LocalDate start, LocalDate end, Set<DayOfWeek> userDays) {
        // When
        boolean result = matcher.isEventAvailableOnDays(eventTime, start, end, userDays);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    private static Stream<Arguments> provideConditionalEventTimes() {
        // 기준 일자 설정 (2026년 기준)
        // 2026-06-26 (금) / 2026-06-27 (토) / 2026-06-28 (일)
        LocalDate fri = LocalDate.of(2026, 6, 26);
        LocalDate sat = LocalDate.of(2026, 6, 27);
        LocalDate sun = LocalDate.of(2026, 6, 28);
        LocalDate mon = LocalDate.of(2026, 6, 29);

        return Stream.of(
                // A. 요일 일치 케이스
                Arguments.of(true, "수 19:30", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), Set.of(DayOfWeek.WEDNESDAY)),
                Arguments.of(true, "화-금 19:30 / 주말, 공휴일 14:00", LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2), Set.of(DayOfWeek.THURSDAY)), // 범위형(목) 통과
                Arguments.of(true, "금 19:30 토,일 15:00", fri, fri, Set.of(DayOfWeek.FRIDAY)),

                // B. 요일 불일치 탈락 케이스
                Arguments.of(false, "토요일 14:00 / 16:30", fri, fri, Set.of(DayOfWeek.FRIDAY)), // 금요일 여행자인데 토요 공연만 존재
                Arguments.of(false, "매주 토 10:00 ~13:00", sun, sun, Set.of(DayOfWeek.SUNDAY)), // 일요일 여행자인데 토요 교육만 존재

                // C. 구체적 날짜 매칭 (텍스트에 날짜가 박혀있는 경우)
                Arguments.of(true, "6.26. (금) 19:30 이루어드림 BALLET CONCERT", fri, fri, Set.of(DayOfWeek.FRIDAY)), // 날짜 적중
                Arguments.of(false, "6.26. (금) 19:30 이루어드림 BALLET CONCERT", sat, sat, Set.of(DayOfWeek.SATURDAY)), // 날짜 불일치 탈락

                // D. 휴관일 시스템 작동 (단일 여행일이 하필 휴관일인 경우)
                Arguments.of(false, "11:00, 14:00, 16:30 * 월 공연없음", mon, mon, Set.of(DayOfWeek.MONDAY)), // 월요일 하루 여행인데 월 공연 없음 -> 탈락
                Arguments.of(false, "13:00 ~ 18:00 (일-월 휴관)", sun, sun, Set.of(DayOfWeek.SUNDAY)), // 일요일 하루 여행인데 일요 휴관 -> 탈락
                Arguments.of(false, "화 - 토, 10:00 ~ 18:00 (일, 월, 공휴일 휴관)", mon, mon, Set.of(DayOfWeek.MONDAY)) // 월요일 하루 여행인데 월요 휴관 -> 탈락
        );
    }

    // -------------------------------------------------------------------------
    // 3. 복합 다중 시간선 패턴 단건 정밀 검증
    // -------------------------------------------------------------------------
    @Test
    @DisplayName(" [복합 텍스트 검증] 복잡하게 얽힌 요일별 시간 안내 텍스트도 유저 요일이 포함되어 있다면 통과해야 한다")
    void shouldPass_WhenComplexMixedDaysTextContainsUserDay() {
        // 복잡한 텍스트 타겟
        String complexText = "화,목 15:00, 17:30 수,금 17:00, 19:30 토 13:30, 15:30, 17:30 일 14:00, 16:00";

        // 유저 일정: 수요일 ~ 목요일 여행
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 2);
        Set<DayOfWeek> userDays = Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY);

        boolean result = matcher.isEventAvailableOnDays(complexText, start, end, userDays);

        assertThat(result).isTrue(); // 수, 목 정보가 텍스트에 들어있으므로 true여야 함
    }
}