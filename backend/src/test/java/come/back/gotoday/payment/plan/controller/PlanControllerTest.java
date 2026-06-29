package come.back.gotoday.payment.plan.controller;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.global.exception.GlobalExceptionHandler; // 💡 핸들러 직접 포함을 위해 임포트
import come.back.gotoday.payment.plan.dto.PlanResponse;
import come.back.gotoday.payment.plan.service.PlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class) // 💡 시큐리티/무거운 컨텍스트 부팅을 아예 안 켜고 Mockito만 켭니다.
@DisplayName("PlanController 독립형 유닛 API 테스트")
class PlanControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlanService planService;

    @InjectMocks
    private PlanController planController; // 💡 테스트할 컨트롤러에 서비스 주입

    @BeforeEach
    void setUp() {
        // 💡 주변 설정 싹 무시하고, 오직 PlanController와 예외 처리를 담당할 GlobalExceptionHandler만 묶어서 MockMvc를 수동 빌드합니다.
        // 💡 이렇게 하면 Handler가 null이 나오는 배달 사고가 원천 차단됩니다.
        this.mockMvc = MockMvcBuilders.standaloneSetup(planController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("케이스 1: [정상 조회] 활성화된 요금제 목록을 요청하면 공통 응답 포맷(ApiResponse)에 맞춰 success=true 및 데이터 리스트를 반환한다.")
    void getPlans_Success_ReturnApiResponse() throws Exception {
        // given
        List<PlanResponse> mockPlans = List.of(
                new PlanResponse(1L, "BASIC_PLAN", "기본 멤버십", 9900L),
                new PlanResponse(2L, "PREMIUM_PLAN", "프리미엄 멤버십", 14900L)
        );
        given(planService.getActivePlans()).willReturn(mockPlans);

        // when & then
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("플랜 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].name").value("BASIC_PLAN"))
                .andExpect(jsonPath("$.data[0].displayName").value("기본 멤버십"))
                .andExpect(jsonPath("$.data[0].amount").value(9900L));
    }

    @Test
    @DisplayName("케이스 2: [빈 목록 조회] 판매 중인 플랜이 0개일 때, data 필드가 빈 배열([])로 안전하게 출력된다.")
    void getPlans_EmptyList_ReturnEmptyArrayInData() throws Exception {
        // given
        List<PlanResponse> emptyPlans = List.of();
        given(planService.getActivePlans()).willReturn(emptyPlans);

        // when & then
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("케이스 3: [잘못된 HTTP 메서드] GET 전용 API에 POST로 잘못 접근하면 405 Method Not Allowed 에러가 발생한다.")
    void getPlans_WrongHttpMethod_Return405MethodNotAllowed() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/subscriptions/plans")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("케이스 4: [비즈니스 예외 대응] 서비스 로직 도중 내부 에러 발생 시 GlobalExceptionHandler가 작동하여 규격화된 실패 JSON을 반환한다.")
    void getPlans_ServiceException_ReturnFailErrorResponse() throws Exception {
        // given
        given(planService.getActivePlans())
                .willThrow(new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));

        // when & then
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}