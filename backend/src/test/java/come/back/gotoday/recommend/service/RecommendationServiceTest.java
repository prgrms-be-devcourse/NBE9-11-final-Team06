package come.back.gotoday.recommend.service;

import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.event.service.EventBatchService;
import come.back.gotoday.recommend.engine.VectorEmbeddingEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RecommendationServiceTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName("👥 [초개인화 시각적 검증] 회원별 성향 분석 및 맞춤 추천 결과 매칭 대시보드 로그 출력")
    void verifyDivergentRecommendationsWithVisualLogs() {
        System.out.println("🔄 [준비 단계] 외부 서울시 API로부터 실시간 데이터를 긁어와 빽빽하게 벡터를 채웁니다...");

        try {
//            eventBatchService.syncSeoulEvents();
            System.out.println("✅ [준비 단계] API 데이터 및 임베딩 벡터 적재 완료!");
        } catch (Exception e) {
            System.err.println("❌ [준비 단계] API 동기화 중 오류 발생: " + e.getMessage());
        }


        // ---------------------------------------------------------------------------------
        //  USER 1: 클래식을 사랑하는 영등포/마포 거주 가정 유저
        // ---------------------------------------------------------------------------------
        printUserDashboardHeader(1L, "영등포마포러버", "영등포", "가족 (FAMILY)", "클래식, 뮤지컬/오페라, 콘서트");
        runVisualRecommendationPipeline(1L, "영등포", "클래식, 뮤지컬/오페라, 콘서트", "가족 (FAMILY)");

        // ---------------------------------------------------------------------------------
        //  USER 2: 조용한 전시/미술을 즐기는 나홀로 족 유저
        // ---------------------------------------------------------------------------------
        printUserDashboardHeader(2L, "혼자하는전시", "종로", "혼자 (ALONE)", "전시/미술, 영화");
        runVisualRecommendationPipeline(2L, "종로", "전시/미술, 영화", "혼자 (ALONE)");

        // ---------------------------------------------------------------------------------
        //  USER 3: 신나는 축제와 페스티벌을 찾아다니는 크루 유저
        // ---------------------------------------------------------------------------------
        printUserDashboardHeader(3L, "페스티벌크루", "홍대", "친구 (FRIEND)", "축제-문화/예술, 콘서트");
        runVisualRecommendationPipeline(3L, "홍대", "축제-문화/예술, 콘서트", "친구 (FRIEND)");
    }

    private void runVisualRecommendationPipeline(Long userId, String area, String categories, String companion) {
        int topK = 6;
        try {
            LocalDate oneDayStart = LocalDate.of(2026, 7, 2);
            LocalDate oneDayEnd = LocalDate.of(2026, 7, 5);

            // 💡 갱신 포인트: 넘겨받은 유저 선호 스펙을 기반으로 서비스의 queryText 템플릿 로직 생성
            String queryText = recommendationService.createQueryText(area, categories, companion);

            // 💡 갱신 포인트: 두 번째 인자에 queryText 추가
            List<Long> recommendedIds = recommendationService.getRecommendedEventIds(
                    userId, queryText, oneDayStart, oneDayEnd, topK);

            assertThat(recommendedIds).isNotEmpty();

            List<Event> recommendedEvents = eventRepository.findAllByIdsWithCategoryAndPlace(recommendedIds);
            System.out.println("│");
            System.out.println("├── 🚀 [하이브리드 추천 매칭서 가동 결과]");
            System.out.println("│   └── AI 매칭 스코어 기준 최적의 추천 행사 순위:");
            System.out.println("│");

            for (int i = 0; i < recommendedEvents.size(); i++) {
                Event ev = recommendedEvents.get(i);
                String categoryName = ev.getCategory() != null ? ev.getCategory().getName() : "미분류";
                String placeName = ev.getPlace() != null ? ev.getPlace().getName() : "공연장 인프라 정보 없음 (API 주소 기반)";
                String fee = (ev.getFee() == null || ev.getFee().isBlank()) ? "정보없음/무료" : ev.getFee();

                System.out.printf("├── 🎫 [%d순위 추천] 행사 ID: %d\n", i + 1, ev.getId());
                System.out.printf("│   ├── 📌 행사제목 : %s\n", ev.getTitle());
                System.out.printf("│   ├── 🏷️ 카테고리 : %s\n", categoryName);
                System.out.printf("│   ├── 📍 행사장소 : %s\n", placeName);
                System.out.printf("│   ├── 📅 행사기간 : %s ~ %s\n", ev.getStartDate(), ev.getEndDate());
                System.out.printf("│   └── 💰 이용요금 : %s\n", fee);
                System.out.println("│");
            }

            System.out.println("=========================================================================================");

        } catch (Exception e) {
            System.out.println("│   ❌ [ERROR] 해당 유저의 추천 파이프라인 가동 중 예외 발생!");
            System.out.println("│   👉 사유: " + e.getMessage());
            System.out.println("=========================================================================================");
        }
    }

    private void printUserDashboardHeader(Long userId, String nickname, String area, String companion, String categories) {
        System.out.println("\n=========================================================================================");
        System.out.printf("👤 [개인화 추천 대상 프로필] 회원 ID: %d | 닉네임: %s\n", userId, nickname);
        System.out.println("=========================================================================================");
        System.out.printf("🗺️ 선호 지역   : %s\n", area);
        System.out.printf("👥 동행인 유형 : %s\n", companion);
        System.out.printf("🎭 관심 카테고리: [ %s ]\n", categories);
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println("🔍 추출된 유저 텐서 및 키워드를 기반으로 하이브리드 벡터 유사도 스캔 중...");
    }


}