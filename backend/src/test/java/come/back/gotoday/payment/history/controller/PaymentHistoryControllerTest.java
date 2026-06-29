package come.back.gotoday.payment.history.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.payment.history.dto.PaymentHistoryResponse;
import come.back.gotoday.payment.history.enums.PaymentStatus;
import come.back.gotoday.payment.history.service.PaymentHistoryFacade;
import come.back.gotoday.payment.history.service.PaymentHistoryService;
import come.back.gotoday.payment.subscription.dto.SubscriptionPaymentCancelRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PaymentHistoryController ")
class PaymentHistoryControllerTest {

    private MockMvc mockMvc;
    private PaymentHistoryService paymentHistoryService;
    private PaymentHistoryFacade paymentHistoryFacade;
    private ObjectMapper objectMapper;

    private CustomUserDetails testUserDetails;
    private final Long memberId = 1L;

    @BeforeEach
    void setUp() {
        paymentHistoryService = mock(PaymentHistoryService.class);
        paymentHistoryFacade = mock(PaymentHistoryFacade.class);
        objectMapper = new ObjectMapper();

        // Jackson이 LocalDateTime을 정상적으로 직렬화할 수 있도록 모듈 등록 (필요시 전역 설정에 맞게 사용)
        objectMapper.findAndRegisterModules();

        // 1. Mock Member 및 시큐리티 UserDetails 세팅
        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(memberId);
        given(mockMember.getEmail()).willReturn("user@gotoday.com");
        given(mockMember.getRole()).willReturn("USER");

        testUserDetails = new CustomUserDetails(mockMember);

        // 2. SecurityContextHolder에 인증 정보 주입
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. 컨트롤러 단독 빌드 및 @AuthenticationPrincipal 리졸버 등록
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentHistoryController(paymentHistoryService, paymentHistoryFacade))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("내 결제 내역 리스트 조회 API (GET /api/v1/subscriptions/payments/me)")
    class GetMyPaymentHistoriesTest {

        @Test
        @DisplayName("성공: 실제 PaymentHistoryResponse 구조에 맞게 필드를 매핑하여 200 OK를 반환한다.")
        void getMyPaymentHistories_Success() throws Exception {
            // given
            // 제공해주신 record 구조 및 빌더 패턴을 정확히 활용하여 Mock 데이터 생성
            PaymentHistoryResponse history1 = PaymentHistoryResponse.builder()
                    .paymentHistoryId(10L)
                    .orderId("ORD-SUB-100")
                    .amount(9900L)
                    .status(PaymentStatus.SUCCESS) // 수정한 부분
                    .receiptUrl("https://receipt.toss.im/abc")
                    .failureReason(null)
                    .createdAt(LocalDateTime.now())
                    .build();

            PaymentHistoryResponse history2 = PaymentHistoryResponse.builder()
                    .paymentHistoryId(11L)
                    .orderId("ORD-SUB-101")
                    .amount(9900L)
                    .status(PaymentStatus.FAILED) // 실패 케이스도 촘촘하게 검증에 포함
                    .receiptUrl(null)
                    .failureReason("잔액 부족")
                    .createdAt(LocalDateTime.now())
                    .build();

            List<PaymentHistoryResponse> mockResponses = List.of(history1, history2);
            given(paymentHistoryService.getPaymentHistories(memberId)).willReturn(mockResponses);

            // when & then
            mockMvc.perform(get("/api/v1/subscriptions/payments/me")
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    // 정확해진 필드명 반영 (id -> paymentHistoryId)
                    .andExpect(jsonPath("$.data[0].paymentHistoryId").value(10L))
                    .andExpect(jsonPath("$.data[0].orderId").value("ORD-SUB-100"))
                    .andExpect(jsonPath("$.data[0].status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data[0].receiptUrl").value("https://receipt.toss.im/abc"))

                    .andExpect(jsonPath("$.data[1].paymentHistoryId").value(11L))
                    .andExpect(jsonPath("$.data[1].status").value("FAILED"))
                    .andExpect(jsonPath("$.data[1].failureReason").value("잔액 부족"))
                    .andExpect(jsonPath("$.message").value("결제 내역 조회가 완료되었습니다."));

            verify(paymentHistoryService).getPaymentHistories(memberId);
        }
    }

    @Nested
    @DisplayName("특정 결제 건 취소 API (POST /api/v1/subscriptions/payments/{paymentHistoryId}/cancel)")
    class CancelPaymentTest {

        @Test
        @DisplayName("성공: 경로 변수와 유효한 취소 사유 바디가 들어오면 결제를 취소하고 200 OK를 반환한다.")
        void cancelPayment_Success() throws Exception {
            // given
            Long paymentHistoryId = 500L;
            SubscriptionPaymentCancelRequest request = new SubscriptionPaymentCancelRequest("고객 변심 취소");

            willDoNothing().given(paymentHistoryFacade)
                    .cancelPayment(eq(memberId), eq(paymentHistoryId), any(SubscriptionPaymentCancelRequest.class));

            // when & then
            mockMvc.perform(post("/api/v1/subscriptions/payments/{paymentHistoryId}/cancel", paymentHistoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("결제 취소 요청이 정상적으로 처리되었습니다."));

            verify(paymentHistoryFacade).cancelPayment(eq(memberId), eq(paymentHistoryId), any(SubscriptionPaymentCancelRequest.class));
        }

        @Test
        @DisplayName("실패: 유효성 검증 위반 - 필수 바디(사유 등)가 비어있으면 400 에러가 발생한다.")
        void cancelPayment_Fail_ValidationError() throws Exception {
            // given
            Long paymentHistoryId = 500L;
            SubscriptionPaymentCancelRequest invalidRequest = new SubscriptionPaymentCancelRequest("");

            // when & then
            mockMvc.perform(post("/api/v1/subscriptions/payments/{paymentHistoryId}/cancel", paymentHistoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isBadRequest());

            verify(paymentHistoryFacade, never()).cancelPayment(any(), any(), any());
        }
    }
}