package come.back.gotoday.event.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class EventScheduleMatcher {

    // 날짜 매칭용 정규식 패턴 (예: "6.26", "7.18", "6/13")
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})[./](\\d{1,2})");

    /**
     * 사용자의 여행 기간(시작일~종료일) 내에 존재하는 모든 DayOfWeek(요일) 리스트를 추출합니다.
     */
    public Set<DayOfWeek> getDaysOfWeekInPeriod(LocalDate start, LocalDate end) {
        Set<DayOfWeek> days = new HashSet<>();
        LocalDate current = start;
        int safetyCounter = 0;
        while (!current.isAfter(end) && safetyCounter < 31) {
            days.add(current.getDayOfWeek());
            current = current.plusDays(1);
            safetyCounter++;
        }
        return days;
    }

    /**
     * 비정형 eventTime 텍스트가 유저의 여행 조건(기간 및 요일) 내에 매칭되는지 정밀 검증합니다.
     */
    public boolean isEventAvailableOnDays(String eventTime, LocalDate searchStart, LocalDate searchEnd, Set<DayOfWeek> userDays) {
        if (eventTime == null || eventTime.isBlank()) {
            return true;
        }

        // 공백 제거 및 소문자화
        String cleanTime = eventTime.replaceAll("\\s+", "");

        // -------------------------------------------------------------------------
        // 1. 구체적인 특정 날짜 표기 패턴 방어 (예: "6.26. (금)", "7.18. (토) / 7.19. (일)")
        // -------------------------------------------------------------------------
        String dateCheckTarget = cleanTime.replaceAll(":\\d{1,2}/\\d{1,2}", "")  // "1:00/3:00" 예외 제거
                .replaceAll("\\d{1,2}/\\d{1,2}:", ""); // "2:30/3:00" 예외 제거

        // 이제 "월.일" 또는 시간과 무관한 "월/일" 패턴만 매칭됩니다.
        if (dateCheckTarget.matches(".*\\d{1,2}[./]\\d{1,2}.*")) {
            boolean dateMatched = isSpecificDateMatched(cleanTime, searchStart, searchEnd);
            // 만약 문자열에 날짜가 명시되어 있고, 유저의 일정 기간과 겹치는 날짜가 '단 하나도' 없다면 즉시 탈락
            if (!dateMatched) {
                return false;
            }
        }

        // -------------------------------------------------------------------------
        // 2. 휴관일 / 제외 요일 패턴 방어 (예: "* 월 공연없음", "일-월 휴관")
        // -------------------------------------------------------------------------
        if (cleanTime.contains("휴관") || cleanTime.contains("공연없음") || cleanTime.contains("제외")) {
            for (DayOfWeek day : userDays) {
                // 유저가 여행하는 요일이 하필 이 행사의 '휴관/제외 요일'에 걸리는지 확인
                if (isTargetDayInText(day, cleanTime, "휴관") ||
                        isTargetDayInText(day, cleanTime, "공연없음") ||
                        isTargetDayInText(day, cleanTime, "제외")) {

                    // 만약 유저의 여행 일정이 단 하루뿐인데 그날이 휴관일이면 탈락
                    if (userDays.size() == 1) {
                        return false;
                    }
                }
            }
        }

        // -------------------------------------------------------------------------
        // 3. 상시 진행 및 단순 시간 포맷 방어 (예: "19:30", "10:00~18:00")
        // -------------------------------------------------------------------------
        if (cleanTime.contains("상시") || cleanTime.contains("매일") || cleanTime.contains("연중") ||
                cleanTime.contains("참조") || cleanTime.contains("상이") || cleanTime.contains("홈페이지")) {
            return true;
        }
        boolean hasAnyDayKeyword = cleanTime.contains("월") || cleanTime.contains("화") ||
                cleanTime.contains("수") || cleanTime.contains("목") ||
                cleanTime.contains("금") || cleanTime.contains("토") ||
                cleanTime.contains("일") || cleanTime.contains("평일") ||
                cleanTime.contains("주말");

        if (!hasAnyDayKeyword) {
            return true; // 요일 제한이 없는 행사이므로 유저가 언제 가든 관람 가능!
        }

        // -------------------------------------------------------------------------
        // 4. 복합/분할 요일 패턴 정밀 매칭 (예: "화,목", "화-금", "토,일", "평일", "주말")
        // -------------------------------------------------------------------------
        boolean mentionsWeekend = cleanTime.contains("토") || cleanTime.contains("일") || cleanTime.contains("주말");
        boolean mentionsWeekday = cleanTime.contains("월") || cleanTime.contains("화") ||
                cleanTime.contains("수") || cleanTime.contains("목") ||
                cleanTime.contains("금") || cleanTime.contains("평일");

        // "화-금", "화~목" 같은 범위형 표현 처리
        if (cleanTime.contains("-") || cleanTime.contains("~")) {
            if (isRangeMatch(cleanTime, userDays)) {
                return true;
            }
        }

        // 단건 혹은 콤마(,) 분할 요일 매칭 ("화,목" 등 문자열 내 유저 요일 존재 여부 체크)
        boolean hasMatchingDay = false;
        for (DayOfWeek day : userDays) {
            if (matchKoreanDayKeyword(day, cleanTime)) {
                hasMatchingDay = true;
                break;
            }
        }

        // 요일 단어들이 명시되어 있는데 유저 일정 요일과 단 하나도 교집합이 없다면 하드 필터링 탈락
        if (mentionsWeekend || mentionsWeekday) {
            return hasMatchingDay;
        }

        return true;
    }

    /**
     * 텍스트에 등장하는 날짜(월.일)가 유저의 여행 기간(searchStart ~ searchEnd) 내에 포함되는지 검증
     */
    private boolean isSpecificDateMatched(String text, LocalDate start, LocalDate end) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        int currentYear = start.getYear(); // 유저가 조회한 연도 기준 (2026년 등)

        boolean hasAnyDateMatch = false;
        while (matcher.find()) {
            try {
                int month = Integer.parseInt(matcher.group(1));
                int day = Integer.parseInt(matcher.group(2));

                LocalDate eventSpecificDate = LocalDate.of(currentYear, month, day);

                // 이벤트 타겟 날짜가 유저의 시작일과 종료일 사이에 있다면 true
                if (!eventSpecificDate.isBefore(start) && !eventSpecificDate.isAfter(end)) {
                    hasAnyDateMatch = true;
                }
            } catch (Exception e) {
                // 날짜 파싱 에러 시 안전하게 통과 추동을 위해 무시
            }
        }
        return hasAnyDateMatch;
    }

    /**
     * "화-금", "화~토" 와 같은 요일 범위 표현을 해석하여 유저 요일과 매칭하는 로직
     */
    private boolean isRangeMatch(String text, Set<DayOfWeek> userDays) {
        String[] daysSequence = {"월", "화", "수", "목", "금", "토", "일"};

        for (int i = 0; i < daysSequence.length; i++) {
            for (int j = i + 1; j < daysSequence.length; j++) {
                String rangeKey1 = daysSequence[i] + "-" + daysSequence[j];
                String rangeKey2 = daysSequence[i] + "~" + daysSequence[j];

                if (text.contains(rangeKey1) || text.contains(rangeKey2)) {
                    // 유저의 요일 중 하나라도 해당 범위에 속하는지 체크
                    for (DayOfWeek userDay : userDays) {
                        int userDayIdx = userDay.getValue() - 1; // 0(월) ~ 6(일)
                        if (userDayIdx >= i && userDayIdx <= j) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 특정 문맥(휴관 등) 근처에 해당 요일 키워드가 붙어있는지 확인하는 피처
     */
    private boolean isTargetDayInText(DayOfWeek day, String text, String contextKeyword) {
        if (!text.contains(contextKeyword)) return false;
        return matchKoreanDayKeyword(day, text);
    }

    private boolean matchKoreanDayKeyword(DayOfWeek day, String text) {
        return switch (day) {
            case MONDAY -> text.contains("월");
            case TUESDAY -> text.contains("화");
            case WEDNESDAY -> text.contains("수");
            case THURSDAY -> text.contains("목");
            case FRIDAY -> text.contains("금") || text.contains("평일");
            case SATURDAY -> text.contains("토") || text.contains("주말");
            case SUNDAY -> text.contains("일") || text.contains("주말");
        };
    }
}