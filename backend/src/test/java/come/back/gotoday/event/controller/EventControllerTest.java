package come.back.gotoday.event.controller;


import come.back.gotoday.event.dto.EventDetailResponse;
import come.back.gotoday.event.dto.EventListResponse;
import come.back.gotoday.event.dto.EventSearchRequest;
import come.back.gotoday.event.service.EventService;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = EventController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class} // 💡 에러를 일으키는 실제 필터 스캔 제외!
        )
)
@WithMockUser // 스프링 시큐리티 필터 통과용 기본 가상 유저
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Nested
    @DisplayName("이벤트 상세 조회 API (/api/events/{eventId})")
    class GetEvent {

        @Test
        @DisplayName("[성공] 정상적인 이벤트 ID가 들어오면 200 OK와 상세 정보를 반환한다")
        void success() throws Exception {
            // given
            Long eventId = 185L;
            EventDetailResponse response = new EventDetailResponse(
                    eventId, 100L, "영등포아트홀", 16L, "공연", "클래식",
                    "[영등포문화재단] 마티네콘서트 With 금난새 #3.베버",
                    LocalDate.of(2026, 6, 11), LocalDate.of(2026, 10, 15),
                    "11:00", "전석 15,000원", "초등학생 이상",
                    "https://homepage.com", "https://image.com",
                    "상세설명", "SEOUL_API", "영등포구", 37.525547, 126.8967
            );
            given(eventService.getEvent(eventId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/events/{eventId}", eventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("이벤트 조회에 성공했습니다."))
                    .andExpect(jsonPath("$.data.id").value(eventId))
                    .andExpect(jsonPath("$.data.title").value("[영등포문화재단] 마티네콘서트 With 금난새 #3.베버"))
                    .andExpect(jsonPath("$.data.placeName").value("영등포아트홀"))
                    .andExpect(jsonPath("$.data.categoryName").value("공연"))
                    .andExpect(jsonPath("$.data.eventCategory").value("클래식"));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 이벤트 ID가 들어오면 서비스 예외를 그대로 예외 처리기(Handler)로 넘긴다")
        void fail_notFound() throws Exception {
            // given
            Long notFoundEventId = 999L;
            given(eventService.getEvent(notFoundEventId))
                    .willThrow(new BusinessException(ErrorCode.EVENT_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/events/{eventId}", notFoundEventId)
                            .with(csrf()))
                    .andDo(print())
                    // GlobalExceptionHandler의 설정에 맞추어 status 검증 (e.g., status().isNotFound() 또는 isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
        }
    }


    @Nested
    @DisplayName("이벤트 목록(다건) 조회 API (/api/events)")
    class GetEvents {

        @Test
        @DisplayName("[성공] 검색 조건과 페이징 파라미터가 주어지면 필터링된 이벤트 목록을 페이징 형태로 반환한다")
        void success_withFilters() throws Exception {
            // given
            EventListResponse eventSample = new EventListResponse(
                    185L, "[영등포문화재단] 마티네콘서트",
                    LocalDate.of(2026, 6, 11), LocalDate.of(2026, 10, 15),
                    "11:00", "영등포구", "https://image.com", "공연", "클래식"
            );

            Page<EventListResponse> mockPage = new PageImpl<>(
                    List.of(eventSample),
                    PageRequest.of(0, 10),
                    1 // 전체 토탈 데이터 수
            );

            // 어떤 검색 요청이 오든 준비한 페이징 데이터를 응답하도록 설정
            given(eventService.getEventList(any(EventSearchRequest.class))).willReturn(mockPage);

            // when & then
            mockMvc.perform(get("/api/events")
                            .param("area", "영등포구")
                            .param("categoryId", "16")
                            .param("keyword", "금난새")
                            .param("status", "ING")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("이벤트 목록 검색에 성공했습니다."))
                    // Spring Data Page 객체의 기본 구조 검증
                    .andExpect(jsonPath("$.data.content[0].id").value(185L))
                    .andExpect(jsonPath("$.data.content[0].title").value("[영등포문화재단] 마티네콘서트"))
                    .andExpect(jsonPath("$.data.content[0].area").value("영등포구"))
                    .andExpect(jsonPath("$.data.content[0].categoryName").value("공연"))
                    .andExpect(jsonPath("$.data.content[0].eventCategory").value("클래식"))
                    .andExpect(jsonPath("$.data.pageable.pageNumber").value(0))
                    .andExpect(jsonPath("$.data.pageable.pageSize").value(10))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("[성공] 파라미터가 전부 비어있어도 DTO 기본 값(page=0, size=10)이 작동하여 정상 조회된다")
        void success_emptyParams() throws Exception {
            // given
            Page<EventListResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(eventService.getEventList(any(EventSearchRequest.class))).willReturn(emptyPage);

            // when & then
            mockMvc.perform(get("/api/events") // 💡 아무 쿼리 스트링도 보내지 않음
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.pageable.pageNumber").value(0))
                    .andExpect(jsonPath("$.data.pageable.pageSize").value(10));
        }
    }
}