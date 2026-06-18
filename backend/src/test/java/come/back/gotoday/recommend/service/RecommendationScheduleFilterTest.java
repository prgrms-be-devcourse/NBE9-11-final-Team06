package come.back.gotoday.recommend.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository; // 프로젝트 실제 패키지 경로에 맞게 확인 필요
import come.back.gotoday.category.type.CategoryType;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.enums.EventSource;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RecommendationScheduleFilterTest {

    private static final Logger log = LoggerFactory.getLogger(RecommendationScheduleFilterTest.class);

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;
    private Member testMember;

    // 공통 모크 위경도 데이터 및 랭킹 엔진 파라미터 정의
    private final Double defaultLat = 37.5665;
    private final Double defaultLng = 126.9780;
    private final boolean defaultAvoidCrowds = false;
    private final int defaultTopK = 10;

    @BeforeEach
    void setUp() {
        // 1. 제공된 테이블 구조 규칙 반영: Category 정적 팩토리 메서드를 통해 생성 및 저장
        testCategory = Category.create("전시/미술", CategoryType.EVENT); // CategoryType 구조에 맞게 매핑 필요
        categoryRepository.save(testCategory);

        // 2. 제공된 테이블 구조 규칙 반영: Member 정적 팩토리 메서드로 필수 제약조건(not null) 충족하여 생성
        testMember = Member.create(
                "tester_unique_email@gotoday.com", // email (unique)
                "securePassword123!",              // password
                "스케줄테스터",                      // nickname (unique)
                "ROLE_USER",                       // role
                "ACTIVE"                           // status
        );
        memberRepository.save(testMember);
    }

    @Test
    @DisplayName("[정상 요일 매칭] 유저의 여행 요일 내에 운영하는 비정형 시간의 행사는 정상적으로 추천되어야 한다")
    void shouldIncludeEvent_WhenEventTimeMatchesUserSubPeriod() {
        // Given: 유저의 여행 일정 (2026년 7월 2일 목요일 ~ 2026년 7월 3일 금요일) -> 목, 금요일 스캔
        LocalDate start = LocalDate.of(2026, 7, 2);
        LocalDate end = LocalDate.of(2026, 7, 3);

        // 목요일에 진행하는 행사와 주말에만 진행하는 행사 적재
        Event matchingEvent = createEvent("목요 아트 갤러리", "마포", testCategory, "매주 목요일 진행", start, end);
        Event nonMatchingEvent = createEvent("주말 도슨트 투어", "마포", testCategory, "토요일, 일요일만 진행", start, end);
        eventRepository.saveAll(List.of(matchingEvent, nonMatchingEvent));

        String queryText = recommendationService.createQueryText("마포", "전시/미술", "누구나");

        // When: 변경된 9개의 파라미터 구조에 맞추어 연동 호출 수행
        List<Long> resultIds = recommendationService.getRecommendedEventIds(
                "마포",
                List.of("전시/미술"),
                queryText,
                start,
                end,
                defaultLat,
                defaultLng,
                defaultAvoidCrowds,
                defaultTopK
        );

        // Then: 유저의 평일 일정과 매칭되는 '목요 아트 갤러리'만 반환되어야 함
        assertThat(resultIds).contains(matchingEvent.getId());
        assertThat(resultIds).doesNotContain(nonMatchingEvent.getId());
    }

    @Test
    @DisplayName("[선택 B 정책 검증] 요일 필터링 결과 매칭 행사가 0건일 때, 필터를 해제하고 차선책(원본)을 복구해야 한다")
    void shouldFallbackToOriginalCandidates_WhenDayOfWeekFilterReturnsEmpty() {
        // Given: 유저의 일정은 2026년 7월 2일 (목요일) 단 하루
        LocalDate start = LocalDate.of(2026, 7, 2);
        LocalDate end = LocalDate.of(2026, 7, 2);

        // 목요일과 겹치지 않는 금요일 운영 행사만 존재
        Event fridayEvent = createEvent("금요 상설 밤도깨비 야시장", "종로", testCategory, "매주 금요일 운영", start, end);
        eventRepository.save(fridayEvent);

        String queryText = recommendationService.createQueryText("종로", "전시/미술", "누구나");

        // When: 변경된 9개의 파라미터 구조에 맞추어 연동 호출 수행
        List<Long> resultIds = recommendationService.getRecommendedEventIds(
                "종로",
                List.of("전시/미술"),
                queryText,
                start,
                end,
                defaultLat,
                defaultLng,
                defaultAvoidCrowds,
                defaultTopK
        );

        // Then: 하드 필터 매칭 결과는 0건이지만 '선택 B 정책'에 의해 요일 필터가 해제되어 fridayEvent가 최종 구제되어야 함
        assertThat(resultIds).isNotEmpty();
        assertThat(resultIds).contains(fridayEvent.getId());
    }

    @Test
    @DisplayName("[휴관일 패턴 검증] 유저 일정 요일이 휴관일에 걸리고 단일 일정(하루 여행)인 경우 추천에서 탈락해야 한다")
    void shouldExcludeEvent_WhenUserSingleDayIsClosedDayOfEvent() {
        // Given: 유저 일정 2026년 7월 6일 (월요일) 단 하루
        LocalDate start = LocalDate.of(2026, 7, 6);
        LocalDate end = LocalDate.of(2026, 7, 6);

        Event closedEvent = createEvent("월요일 닫는 팝업스토어", "종로", testCategory, "매주 월요일 휴관합니다.", start, end);
        Event openEvent = createEvent("365 상시 개방 전시", "종로", testCategory, "상시 진행 (연중무휴)", start, end);
        eventRepository.saveAll(List.of(closedEvent, openEvent));

        String queryText = recommendationService.createQueryText("종로", "전시/미술", "누구나");

        // When: 변경된 9개의 파라미터 구조에 맞추어 연동 호출 수행
        List<Long> resultIds = recommendationService.getRecommendedEventIds(
                "종로",
                List.of("전시/미술"),
                queryText,
                start,
                end,
                defaultLat,
                defaultLng,
                defaultAvoidCrowds,
                defaultTopK
        );

        // Then: 월요일 휴관인 행사는 완벽히 제외되고 연중무휴 행사만 노출되어야 함
        assertThat(resultIds).contains(openEvent.getId());
        assertThat(resultIds).doesNotContain(closedEvent.getId());
    }

    @Test
    @DisplayName("[예외 방어 문구] 시간 정보만 있거나 상시 문구가 명시되면 하드 필터를 무조건 프리패스해야 한다")
    void shouldPassFilter_WhenEventTimeIsAlwaysOrTimeFormatOnly() {
        // Given: 유저 일정 2026년 7월 2일 (목요일)
        LocalDate start = LocalDate.of(2026, 7, 2);
        LocalDate end = LocalDate.of(2026, 7, 2);

        Event timeOnlyEvent = createEvent("24시간 오픈 갤러리", "종로", testCategory, "10:00~18:00", start, end);
        Event regularEvent = createEvent("상설 상시 전시", "종로", testCategory, "자세한 일정은 홈페이지 참조 요망 (상시진행)", start, end);
        eventRepository.saveAll(List.of(timeOnlyEvent, regularEvent));

        String queryText = recommendationService.createQueryText("종로", "전시/미술", "누구나");

        // When: 변경된 9개의 파라미터 구조에 맞추어 연동 호출 수행
        List<Long> resultIds = recommendationService.getRecommendedEventIds(
                "종로",
                List.of("전시/미술"),
                queryText,
                start,
                end,
                defaultLat,
                defaultLng,
                defaultAvoidCrowds,
                defaultTopK
        );

        // Then: 상시/시간 전용 예외 처리 룰에 의해 하드 필터를 통과하고 반환되어야 함
        assertThat(resultIds).contains(timeOnlyEvent.getId(), regularEvent.getId());
    }

    @Test
    @DisplayName(" [2박 3일 정밀 매칭] 유저의 목/금/토 일정 내에 비정형 운영 요일이 걸쳐있는 행사들이 정확히 매칭되는가")
    void verifyThreeDaysTripScheduleMatchingWithVisualLogs() {
        // Given: 유저의 2박 3일 일정 (2026년 7월 2일 목요일 ~ 2026년 7월 4일 토요일)
        LocalDate start = LocalDate.of(2026, 7, 2);
        LocalDate end = LocalDate.of(2026, 7, 4);

        // 다양한 비정형 시간 데이터를 가진 가상 행사 적재
        Event match1 = createEvent("수, 목 상설 도슨트", "종로", testCategory, "수, 목 15:00, 17:30", start, end);
        Event match2 = createEvent("금요 발레 콘서트", "종로", testCategory, "금 19:30 토,일 15:00", start, end);
        Event match3 = createEvent("토요 원데이 클래스", "종로", testCategory, "매주 토 10:00 ~13:00", start, end);
        Event match4 = createEvent("연중무휴 현대미술전", "종로", testCategory, "10:00 ~ 17:50", start, end); // 단순 시간 패턴 (프리패스)

        Event nonMatch1 = createEvent("월요 정기 연주회", "종로", testCategory, "월요일 10:00~11:30", start, end); // 일정 외 요일 (탈락 대상)
        Event nonMatch2 = createEvent("일요 클래식 오케스트라", "종로", testCategory, "일요일 14:00", start, end); // 일정 외 요일 (탈락 대상)

        eventRepository.saveAll(List.of(match1, match2, match3, match4, nonMatch1, nonMatch2));
        String queryText = recommendationService.createQueryText("종로", "전시/미술", "누구나");

        log.info("\n=========================================================================================\n" +
                "[TEST START] 2박 3일 유저 일정 매칭 검증 시뮬레이션\n" +
                "여행 기간: {} ~ {} [목요일, 금요일, 토요일]\n" +
                "=========================================================================================", start, end);

        // When: 변경된 9개의 파라미터 구조에 맞추어 연동 호출 수행
        List<Long> resultIds = recommendationService.getRecommendedEventIds(
                "종로",
                List.of("전시/미술"),
                queryText,
                start,
                end,
                defaultLat,
                defaultLng,
                defaultAvoidCrowds,
                defaultTopK
        );

        // Then & Visual Log Output
        log.info("\n-----------------------------------------------------------------------------------------\n" +
                "📊 [ENGINE MATCHING RESULT] 이번 테스트 타깃 후보군 중 최종 생존 목록\n" +
                "-----------------------------------------------------------------------------------------");

        List<Event> testDummyEvents = List.of(match1, match2, match3, match4, nonMatch1, nonMatch2);

        for (Event event : testDummyEvents) {
            boolean isSurvived = resultIds.contains(event.getId());
            String statusEmoji = isSurvived ? " [PASS]" : "[DROP]";
            log.info("{} 행사명: |{}[비정형 시간: {}]",
                    String.format("%-8s", statusEmoji),
                    String.format("%-18s|", event.getTitle()),
                    event.getEventTime());
        }
        log.info("=========================================================================================");

        // 검증 단언
        assertThat(resultIds).contains(match1.getId(), match2.getId(), match3.getId(), match4.getId());
        assertThat(resultIds).doesNotContain(nonMatch1.getId(), nonMatch2.getId());
    }

    @Test
    @DisplayName("[선택 B 정책 트리거] 매칭되는 행사가 0건일 때 전체 후보군이 구제 복구되는 흐름 로그 확인")
    void verifyFallbackPolicyTriggerWithVisualLogs() {
        // Given: 유저의 일정은 2026년 7월 6일 (월요일) 단 하루
        LocalDate start = LocalDate.of(2026, 7, 6);
        LocalDate end = LocalDate.of(2026, 7, 6);

        // DB에는 월요일 휴관이거나 주말 운영 행사만 존재함 (하드 필터링 시 0건이 보장되는 환경)
        Event weekendEvent = createEvent("주말 플리마켓", "강남", testCategory, "토, 일 14:00", start, end);
        Event closedEvent = createEvent("월요 휴관 박물관", "강남", testCategory, "화 - 토, 10:00 ~ 18:00 (일, 월, 공휴일 휴관)", start, end);
        eventRepository.saveAll(List.of(weekendEvent, closedEvent));

        String queryText = recommendationService.createQueryText("강남", "전시/미술", "누구나");

        log.info("\n=========================================================================================\n" +
                " [TEST START] 선택 B 정책(매칭 실패 시 복구) 시뮬레이션\n" +
                " 여행 기간: {} ~ {} [월요일 단 하루]\n" +
                "=========================================================================================", start, end);

        // When: 변경된 9개의 파라미터 구조에 맞추어 연동 호출 수행
        List<Long> resultIds = recommendationService.getRecommendedEventIds(
                "강남",
                List.of("전시/미술"),
                queryText,
                start,
                end,
                defaultLat,
                defaultLng,
                defaultAvoidCrowds,
                defaultTopK
        );

        // Then & Fallback Process Output
        log.info("\n-----------------------------------------------------------------------------------------\n" +
                "[FALLBACK TRIGGER REPORT] 스케줄 필터 통과 건수: 0건 발견 -> [선택 B 정책] 발동됨\n" +
                "-----------------------------------------------------------------------------------------");

        if (!resultIds.isEmpty()) {
            log.info(" 알림: 하드 요일 필터가 정상적으로 해제되어 기존 데이터 후보군이 구제되었습니다.");
            for (Long id : resultIds) {
                Event savedEvent = eventRepository.findById(id).orElseThrow();
                log.info(" [구제 완료] 행사명: {} | 비정형 시간: {}", savedEvent.getTitle(), savedEvent.getEventTime());
            }
        }
        log.info("=========================================================================================");

        // 하드 필터링은 실패했지만 차선책 구제 정책에 의해 최종 결과 리스트가 비어있지 않아야 함
        assertThat(resultIds).isNotEmpty();
        assertThat(resultIds).contains(weekendEvent.getId(), closedEvent.getId());
    }

    // --- 엔티티 무결성용 정적 팩토리 헬퍼 메서드 ---
    private Event createEvent(String title, String area, Category category, String eventTime, LocalDate start, LocalDate end) {
        float[] dummyEmbedding = new float[]{0.1f, -0.2f, 0.5f, 0.8f}; // 임베딩 연산 널 포인터 방지용 덤프 벡터

        return Event.create(
                null,                  // Place 연관관계
                category,              // Category (nullable = false 대응 완료)
                title,                 // Title (nullable = false 대응 완료)
                start.minusDays(3),    // startDate (유저 요일 스캔 범위 내 안착되도록 기간 확장)
                end.plusDays(3),       // endDate
                eventTime,             // eventTime (테스트 대상 비정형 텍스트)
                "무료",                 // fee
                "누구나",               // target
                "http://test-url.com", // homepageUrl
                "http://test-img.com", // imageUrl
                "행사 상세 설명 테스트",   // description
                EventSource.SEOUL_API, // EventSource Enum 구조 매핑
                "EXT-" + System.nanoTime(), // 외부 연동 ID 고유값 유도
                dummyEmbedding,        // embeddingVector
                area,                  // area
                37.5665,               // latitude
                126.9780               // longitude
        );
    }
}