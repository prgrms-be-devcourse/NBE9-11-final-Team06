package come.back.gotoday.recommend.service;

import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.junit.jupiter.api.Disabled("로컬에서 도커에 직접 값을 넣을 때 사용하는 테스트 코드")
class RealDataRecommendationFilterTest {

    private static final Logger log = LoggerFactory.getLogger(RealDataRecommendationFilterTest.class);

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName(" [로컬 DB 실데이터 검증] 2박 3일 목/금/토 일정에 맞는 실제 행사 매칭 및 하드 필터링 추적")
    void verifyRealDatabaseEventsWithThreeDaysTrip() {
        // Given: 실제 유저가 가고자 하는 2박 3일 일정 (2026년 7월 2일 목요일 ~ 2026년 7월 4일 토요일)
        LocalDate start = LocalDate.of(2026, 7, 2);
        LocalDate end = LocalDate.of(2026, 7, 4);

        // 테스트 타깃: 현재 로컬 DB에 들어있는 '전체 실제 데이터' 개수 파악
        List<Event> allRealEvents = eventRepository.findAll();

        if (allRealEvents.isEmpty()) {
            log.warn("⚠️ [경고] 현재 로컬 DB(gotoday_db)의 event 테이블에 데이터가 비어있습니다. 공공 API 수집을 먼저 진행해주세요.");
            return;
        }

        String sampleArea = "종로";
        String queryText = recommendationService.createQueryText(sampleArea, "전시/미술", "누구나");

        // ✨ [팀원 변경사항 대응]: 새 알고리즘에 필요한 위치 기반 데이터 및 인코딩 옵션 기본값 설정
        Double mockLatitude = 37.5665;   // 서울 중심 위도 기본값
        Double mockLongitude = 126.9780; // 서울 중심 경도 기본값
        boolean avoidCrowds = false;     // 혼잡도 기반 하향 가중치 비활성화 (기본 추천도 + 거리 위주 스캔)
        int topK = 50;                   // 상위 50개 확보

        log.info("\n=========================================================================================\n" +
                        "디비 실데이터 검증 시작 🚀 [REAL DATA INTEGRATION TEST]\n" +
                        "📅 유저 여행 일정: {} ~ {} [목, 금, 토 2박 3일]\n" +
                        "📍 기준 위경도: ({}, {})\n" +
                        "👥 혼잡도 회피 옵션: {}\n" +
                        "🗄️ 현재 로컬 DB 내 총 행사 수: {}개\n" +
                        "=========================================================================================",
                start, end, mockLatitude, mockLongitude, avoidCrowds, allRealEvents.size());

        // When: ✨ 역변한 팀원의 getRecommendedEventIds 파라미터 구조에 맞춰 정밀 호출 완료
        List<Long> recommendedIds = recommendationService.getRecommendedEventIds(
                sampleArea,
                Set.of(17L),
                queryText,
                start,
                end,
                mockLatitude,
                mockLongitude,
                avoidCrowds,
                topK
        );

        // Then: 시각적 대조 분석 콘솔 출력
        log.info("\n-----------------------------------------------------------------------------------------\n" +
                "📊 [REAL-TIME ENGINE REPORT] 최신 랭킹 알고리즘(RRF + Greedy) 최종 선발 결과 리포트\n" +
                "-----------------------------------------------------------------------------------------");

        int passCount = 0;
        int dropCount = 0;

        for (Event event : allRealEvents) {
            boolean isAreaMatch = event.getArea() != null && event.getArea().contains(sampleArea);

            if (isAreaMatch) {
                boolean isSurvived = recommendedIds.contains(event.getId());
                String statusEmoji = isSurvived ? "✅ [PASS]" : "❌ [DROP]";

                if (isSurvived) passCount++; else dropCount++;

                log.info("{} 지역: [{}] | 행사명: {} | [비정형 시간 포맷: {}]",
                        String.format("%-8s", statusEmoji),
                        event.getArea(),
                        String.format("%-25s", truncateString(event.getTitle(), 25)),
                        event.getEventTime());
            }
        }

        log.info("\n=========================================================================================\n" +
                "📈 [FINAL SUMMARY] '{}' 지역 매칭 결과 요약\n" +
                "🎯 다중 점수 조합 및 거리 계산 통과 (PASS): {}개\n" +
                "🛡️ 스케줄 탈락 혹은 순위 경쟁 밀림 (DROP): {}개\n" +
                "=========================================================================================", sampleArea, passCount, dropCount);

        assertThat(recommendedIds).isNotNull();
    }

    private String truncateString(String str, int length) {
        if (str == null) return "N/A";
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }
}