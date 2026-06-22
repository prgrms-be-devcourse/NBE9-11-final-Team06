package come.back.gotoday.recommend.controller;

import come.back.gotoday.recommend.dto.RecommendationCourseCreateRequest;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.course.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("추천 컨트롤러 테스트")
class RecommendationControllerTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private CourseService courseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RecommendationController controller = new RecommendationController(courseService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("유효한 요청이면 추천 코스를 생성하고 201 응답을 반환한다")
    void createRecommendedCourseReturnsCreated() throws Exception {
        LocalDate today = LocalDate.now();

        given(courseService.createRecommendedCourse(
                eq(MEMBER_ID),
                any(RecommendationCourseCreateRequest.class)
        )).willReturn(null);

        mockMvc.perform(post("/api/recommendations/courses")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "서울 데이트 추천 코스",
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "topK": 3,
                                  "area": "강남구",
                                  "categories": ["전시", "카페"],
                                  "companionType": "커플",
                                  "address": "서울특별시 강남구",
                                  "latitude": 37.5665,
                                  "longitude": 126.9780
                                }
                                """.formatted(today, today.plusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("추천 코스 생성 성공"));

        verify(courseService).createRecommendedCourse(
                eq(MEMBER_ID),
                any(RecommendationCourseCreateRequest.class)
        );
    }

    @Test
    @DisplayName("topK가 1보다 작으면 400 응답을 반환한다")
    void createRecommendedCourseReturnsBadRequestWhenTopKIsInvalid() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(post("/api/recommendations/courses")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "서울 데이트 추천 코스",
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "topK": 0,
                                  "area": "강남구",
                                  "categories": ["전시"],
                                  "companionType": "커플"
                                }
                                """.formatted(today, today)))
                .andExpect(status().isBadRequest());

        verify(courseService, never()).createRecommendedCourse(
                eq(MEMBER_ID),
                any(RecommendationCourseCreateRequest.class)
        );
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠르면 400 응답을 반환한다")
    void createRecommendedCourseReturnsBadRequestWhenPeriodIsInvalid() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(post("/api/recommendations/courses")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "서울 데이트 추천 코스",
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "topK": 3,
                                  "area": "강남구",
                                  "categories": ["전시"],
                                  "companionType": "커플"
                                }
                                """.formatted(today.plusDays(2), today.plusDays(1))))
                .andExpect(status().isBadRequest());

        verify(courseService, never()).createRecommendedCourse(
                eq(MEMBER_ID),
                any(RecommendationCourseCreateRequest.class)
        );
    }

    private static class AuthenticationPrincipalArgumentResolver
            implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && (parameter.getParameterType().equals(Long.class)
                    || parameter.getParameterType().equals(CustomUserDetails.class));
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                org.springframework.web.bind.support.WebDataBinderFactory binderFactory
        ) {
            if (parameter.getParameterType().equals(Long.class)) {
                return MEMBER_ID;
            }

            return org.mockito.Mockito.mock(CustomUserDetails.class, invocation -> {
                if (invocation.getMethod().getName().equals("getMemberId")) {
                    return MEMBER_ID;
                }
                return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
            });
        }
    }
}
