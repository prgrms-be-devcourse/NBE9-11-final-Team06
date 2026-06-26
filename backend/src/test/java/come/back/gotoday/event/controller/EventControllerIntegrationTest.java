package come.back.gotoday.event.controller;
import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.type.CategoryType;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.enums.EventSource;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "SEOUL_CROWD_AREA_NAMES=강남역,홍대 관광특구,성수카페거리",
        "crowd.scheduler.enabled=false",
        "TOUR_API_KEY=mock_tour_api_key",
        "external.tour.api-key=mock_tour_api_key",
        "KAKAO_REST_API_KEY=mock_kakao_rest_api_key",
        "external.kakao.api-key=mock_kakao_rest_api_key",
        "KMA_WEATHER_API_KEY=mock_weather_api_key",
        "weather.kma.service-key=mock_weather_api_key"
})
@AutoConfigureMockMvc
@Transactional // 테스트 완료 후 DB 자동 Rollback 보장
@WithMockUser  // 시큐리티 필터 통과용 가상 유저
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em; // 영속화 처리를 위한 엔티티 매니저

    @MockitoBean(name = "apiKeyCheckConfig")
    private Object apiKeyCheckConfig;

    @Nested
    @DisplayName("이벤트 단건 조회 통합 테스트")
    class GetEventIntegration {

        @Test
        @DisplayName("[성공] 정적 팩토리 메서드로 생성된 진짜 DB 데이터를 매칭하여 200 OK와 상세 정보를 반환한다")
        void success() throws Exception {
            // given - 1. Category 생성 (정적 팩토리 메서드 사용)
            Category category = Category.create("클래식", CategoryType.EVENT);
            em.persist(category);

            // given - 2. Place 생성 (정적 팩토리 메서드 사용)
            Place place = Place.create(
                    category,
                    "영등포아트홀",
                    "서울특별시 영등포구 국회대로53길 20",
                    "서울특별시 영등포구 당산동3가 3",
                    new BigDecimal("37.5255470"),
                    new BigDecimal("126.8967000"),
                    "02-2629-2200",
                    "https://www.ydpcf.or.kr",
                    "영등포구 대표 문화예술 공간",
                    "SEOUL_API",
                    "P_100",
                    true
            );
            em.persist(place);

            // given - 3. Event 생성 (정적 팩토리 메서드 사용, EventSource enum 토큰 전달)
            float[] mockVector = new float[]{0.1f, 0.2f, 0.3f};
            Event event = Event.create(
                    place,
                    category,
                    "클래식",
                    "[영등포문화재단] 마티네콘서트 With 금난새 #3.베버",
                    LocalDate.of(2026, 6, 11),
                    LocalDate.of(2026, 10, 15),
                    "11:00",
                    "전석 15,000원",
                    "초등학생 이상",
                    "https://homepage.com",
                    "https://image.com",
                    "상세설명",
                    EventSource.SEOUL_API, // 지정하신 Enum 객체 전달
                    "EV_185",
                    mockVector,
                    "영등포구",
                    37.525547,
                    126.8967
            );
            em.persist(event);

            // 영속성 컨텍스트 플러시 및 초기화를 통해 1차 캐시를 지우고 순수 DB 조회를 유도
            em.flush();
            em.clear();

            Long savedEventId = event.getId();

            // when & then
            mockMvc.perform(get("/api/events/{eventId}", savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("이벤트 조회에 성공했습니다."))
                    .andExpect(jsonPath("$.data.id").value(savedEventId))
                    .andExpect(jsonPath("$.data.title").value("[영등포문화재단] 마티네콘서트 With 금난새 #3.베버"))
                    .andExpect(jsonPath("$.data.placeName").value("영등포아트홀"))
                    .andExpect(jsonPath("$.data.categoryName").value("클래식"));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 가상의 식별자로 상세 조회를 요청하면 EVENT_NOT_FOUND 에러 스펙을 반환한다")
        void fail_notFound() throws Exception {
            // given
            Long notFoundEventId = 99999L;

            // when & then
            mockMvc.perform(get("/api/events/{eventId}", notFoundEventId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.EVENT_NOT_FOUND.name()));
        }
    }


    @Nested
    @DisplayName("이벤트 목록 조회 통합 테스트 (진짜 DB 연동)")
    class GetEventsIntegration {

        @Test
        @DisplayName("[성공] 대상을 필터링하는 파라미터를 던졌을 때, 일치하는 데이터만 정확히 조회해온다")
        void success_filterMatching() throws Exception {
            // given - 1. 카테고리 2종 생성 (클래식, 전시)
            Category classic = Category.create("클래식", CategoryType.EVENT);
            Category exhibition = Category.create("전시", CategoryType.EVENT);
            em.persist(classic);
            em.persist(exhibition);

            LocalDate today = LocalDate.now();
            // given - 2. 영등포구 클래식 이벤트 저장
            Event event1 = Event.create(
                    null, classic, "클래식", "금난새의 클래식 대행진",
                    today.minusDays(1), today.plusDays(30),
                    "19:00", "무료", "전체", "url", "image1", "설명",
                    EventSource.SEOUL_API, "EXT_1", new float[]{0.1f}, "영등포구", 37.5, 126.8
            );
            // given - 3. 마포구 전시 이벤트 저장 (필터에서 제외되어야 할 대상)
            Event event2 = Event.create(
                    null, exhibition, "전시/미술", "마포 현대 미술전",
                    today.minusDays(1), today.plusDays(30),
                    "14:00", "10000원", "전체", "url", "image2", "설명",
                    EventSource.SEOUL_API, "EXT_2", new float[]{0.2f}, "마포구", 37.5, 126.9
            );
            em.persist(event1);
            em.persist(event2);

            em.flush();
            em.clear();

            // when & then: '영등포구'이면서 '클래식(classic.getId())'인 데이터만 조회하라고 요청
            mockMvc.perform(get("/api/events")
                            .param("area", "영등포구")
                            .param("categoryId", classic.getId().toString())
                            .param("keyword", "금난새")
                            .param("status", "ING")
                            .contentType(MediaType.APPLICATION_JSON)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    // event1만 잡혀야 하므로 결과 크기는 1이어야 함
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("금난새의 클래식 대행진"))
                    .andExpect(jsonPath("$.data.content[0].area").value("영등포구"))
                    .andExpect(jsonPath("$.data.content[0].categoryName").value("클래식"));
        }

        @Test
        @DisplayName("[성공] 검색 필터 조건에 매칭되는 데이터가 DB에 전혀 없다면 빈 콘텐트리스트(크기 0)를 반환한다")
        void success_noMatchData() throws Exception {
            // given
            Category classic = Category.create("클래식", CategoryType.EVENT);
            em.persist(classic);

            Event event = Event.create(
                    null, classic, "클래식", "금난새 콘서트",
                    LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1), // 2026년 6월 1일 마감됨
                    "19:00", "유료", "전체", "url", "image", "설명",
                    EventSource.SEOUL_API, "EXT_3", new float[]{0.1f}, "강남구", 37.5, 126.8
            );
            em.persist(event);

            em.flush();
            em.clear();

            // when & then: 현재 날짜(2026년 6월 16일) 기준 진행중('ING')인 데이터를 마포구에서 찾으라고 요청 (결과 없음)
            mockMvc.perform(get("/api/events")
                            .param("area", "존재하지 않는 지역")
                            .param("status", "ING")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(0)) // 일치 데이터 없음
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("[성공] 특정 키워드를 검색어로 던졌을 때, 제목에 해당 키워드가 포함된 이벤트만 정확히 필터링한다")
        void success_keywordSearch() throws Exception {
            // given
            Category classic = Category.create("클래식", CategoryType.EVENT);
            em.persist(classic);

            // 💡 다른 기존 데이터와 겹치지 않도록 독특한 키워드를 제목에 심어줍니다.
            String targetKeyword = "★특수키워드★";

            Event matchEvent = Event.create(
                    null, classic, "클래식", "제1회 " + targetKeyword + " 음악회",
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    "19:00", "무료", "전체", "url", "image1", "설명",
                    EventSource.SEOUL_API, "EXT_K1", new float[]{0.1f}, "영등포구", 37.5, 126.8
            );
            Event nonMatchEvent = Event.create(
                    null, classic, "클래식", "일반적인 클래식 피아노 리사이틀",
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    "19:00", "무료", "전체", "url", "image2", "설명",
                    EventSource.SEOUL_API, "EXT_K2", new float[]{0.2f}, "영등포구", 37.5, 126.8
            );
            em.persist(matchEvent);
            em.persist(nonMatchEvent);

            em.flush();
            em.clear();

            // when & then: 키워드를 검색 조건으로 설정하여 요청
            mockMvc.perform(get("/api/events")
                            .param("keyword", targetKeyword)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    // 특수키워드가 들어간 1건만 결과 리스트에 담겨야 함
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("제1회 ★특수키워드★ 음악회"));
        }

        @Test
        @DisplayName("[성공] 특정 구역에 대량의(12개) 이벤트를 등록하고 page=1, size=5를 요청하면, 페이징 규격에 맞춰 2번째 페이지의 5개 데이터가 다건 반환된다")
        void success_pagingHandling() throws Exception {
            // given
            Category classic = Category.create("클래식", CategoryType.EVENT);
            em.persist(classic);

            // 💡 기존 41개 데이터의 영향을 완전히 배제하기 위해 존재하지 않는 자치구("우주구")를 지정합니다.
            String targetArea = "우주구";
            int totalInsertCount = 12;

            // 우주구에 총 12개의 이벤트를 루프를 돌며 생성 및 저장
            for (int i = 1; i <= totalInsertCount; i++) {
                Event pagingEvent = Event.create(
                        null, classic, "클래식", "우주 정기 공연 " + i,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                        "19:00", "무료", "전체", "url", "image", "설명",
                        EventSource.SEOUL_API, "EXT_P" + i, new float[]{0.1f}, targetArea, 37.5, 126.8
                );
                em.persist(pagingEvent);
            }

            em.flush();
            em.clear();

            // when & then: 12개 중 'size=5'개씩 쪼갰을 때 'page=1'(즉, 2번째 페이지: 6번째~10번째 데이터)을 요청
            mockMvc.perform(get("/api/events")
                            .param("area", targetArea)
                            .param("page", "1")
                            .param("size", "5")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    // 페이징 메타데이터 정합성 검증
                    .andExpect(jsonPath("$.data.totalElements").value(totalInsertCount)) // 우주구 전체 데이터 수 = 12
                    .andExpect(jsonPath("$.data.pageable.pageSize").value(5))            // 한 페이지당 사이즈 = 5
                    .andExpect(jsonPath("$.data.pageable.pageNumber").value(1))          // 현재 페이지 번호 = 1 (0부터 시작하므로 2번째 페이지)
                    // 다건 반환 검증
                    .andExpect(jsonPath("$.data.content.length()").value(5))            // 2번째 페이지에도 5개의 데이터가 가득 차서 다건 반환되어야 함
                    .andExpect(jsonPath("$.data.content[0].title").value(org.hamcrest.Matchers.containsString("우주 정기 공연")))
                    .andExpect(jsonPath("$.data.content[4].title").value(org.hamcrest.Matchers.containsString("우주 정기 공연")));
        }


        @Test
        @DisplayName("[성공] 모든 검색 파라미터가 null일 때, 에러 없이 기본 페이징 조건에 맞춰 전체 데이터를 조회한다")
        void success_allParametersNull() throws Exception {
            // given - 기존 DB에 41개의 데이터가 이미 마이그레이션 되어 있는 상태 가정

            // when & then: 파라미터를 전혀 붙이지 않고 호출
            mockMvc.perform(get("/api/events")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    // EventSearchRequest의 기본 생성자에 의해 page=0, size=10으로 동작해야 함
                    .andExpect(jsonPath("$.data.pageable.pageSize").value(10))
                    .andExpect(jsonPath("$.data.pageable.pageNumber").value(0))
                    .andExpect(jsonPath("$.data.content.length()").value(10)); // 최대 사이즈만큼 다건 반환
        }

        @Test
        @DisplayName("[성공] 상태 필터가 'ING'일 때, 오늘(2026-06-16) 종료되는 이벤트는 포함되고 어제(2026-06-15) 종료된 이벤트는 제외된다")
        void success_statusDateBoundary() throws Exception {
            // given
            Category classic = Category.create("클래식", CategoryType.EVENT);
            em.persist(classic);

            String boundaryArea = "경계구";

            LocalDate today = LocalDate.now();
            // 1. 오늘 마감되는 이벤트 (ING에 포함되어야 함)
            Event todayEndEvent = Event.create(
                    null, classic, "클래식", "오늘 마감 공연",
                    today.minusDays(15), today, // 오늘 마감
                    "19:00", "무료", "전체", "url", "image", "설명",
                    EventSource.SEOUL_API, "EXT_B1", new float[]{0.1f}, boundaryArea, 37.5, 126.8
            );
            // 2. 어제 마감된 이벤트 (ING에서 제외되어야 함)
            Event yesterdayEndEvent = Event.create(
                    null, classic, "클래식", "어제 마감된 공연",
                    today.minusDays(15), today.minusDays(1), // 어제 마감
                    "19:00", "무료", "전체", "url", "image", "설명",
                    EventSource.SEOUL_API, "EXT_B2", new float[]{0.1f}, boundaryArea, 37.5, 126.8
            );
            em.persist(todayEndEvent);
            em.persist(yesterdayEndEvent);

            em.flush();
            em.clear();

            // when & then: '경계구' 안에서 진행중('ING')인 데이터를 요청
            mockMvc.perform(get("/api/events")
                            .param("area", boundaryArea)
                            .param("status", "ING")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1)) // 오늘 마감 데이터 1건만 나와야 함
                    .andExpect(jsonPath("$.data.content[0].title").value("오늘 마감 공연"));
        }

        @Test
        @DisplayName("[성공] 실제 데이터 보유 페이지 범위를 초과하여 page=100을 요청하면 에러 없이 빈 content 리스트를 반환한다")
        void success_pageOverflowReturnsEmpty() throws Exception {
            // given
            Category classic = Category.create("클래식", CategoryType.EVENT);
            em.persist(classic);

            String overflowArea = "오버플로우구";
            Event event = Event.create(
                    null, classic, "클래식", "공연",
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    "19:00", "무료", "전체", "url", "image", "설명",
                    EventSource.SEOUL_API, "EXT_O1", new float[]{0.1f}, overflowArea, 37.5, 126.8
            );
            em.persist(event);

            em.flush();
            em.clear();

            // when & then: 데이터는 1건(0페이지)뿐인데 100페이지를 요구
            mockMvc.perform(get("/api/events")
                            .param("area", overflowArea)
                            .param("page", "100")
                            .param("size", "10")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(0)); // 터지지 않고 빈 배열 서빙 확인
        }
    }
}