package come.back.gotoday.payment.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.payment.billing.dto.BillingDetailsResponse;
import come.back.gotoday.payment.billing.dto.BillingIssueRequest;
import come.back.gotoday.payment.billing.dto.BillingIssueResponse;
import come.back.gotoday.payment.billing.service.BillingFacade;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("BillingController 단위 테스트")
class BillingControllerTest {

    private MockMvc mockMvc;
    private BillingFacade billingFacade;
    private ObjectMapper objectMapper;

    private CustomUserDetails testUserDetails;
    private final Long memberId = 1L;
    private final String testIdempotencyKey = "sample-idempotency-key-uuid";

    @BeforeEach
    void setUp() {
        billingFacade = org.mockito.Mockito.mock(BillingFacade.class);
        objectMapper = new ObjectMapper();

        // Mock Member 설정
        Member mockMember = org.mockito.Mockito.mock(Member.class);
        given(mockMember.getId()).willReturn(memberId);
        given(mockMember.getEmail()).willReturn("test@gotoday.com");
        given(mockMember.getPassword()).willReturn("hashed_password");
        given(mockMember.getRole()).willReturn("USER");
        given(mockMember.isDeleted()).willReturn(false);

        testUserDetails = new CustomUserDetails(mockMember);

        // SecurityContext 인증 객체 주입
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // @AuthenticationPrincipal 처리를 위해 리졸버 등록하여 MockMvc 구축
        mockMvc = MockMvcBuilders.standaloneSetup(new BillingController(billingFacade))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("빌링키 발급 API (POST /api/v1/billing/issue)")
    class IssueBillingKeyTest {

        @Test
        @DisplayName("성공: 필수 멱등키 헤더와 올바른 바디가 들어오면 빌링키를 발급하고 200 OK를 반환한다.")
        void issueBillingKey_Success() throws Exception {
            // given
            BillingIssueRequest request = new BillingIssueRequest("test_auth_key", "test_customer_key");
            BillingIssueResponse response = new BillingIssueResponse(100L, "신한카드", "1234-5678-****-****");

            // 패사드 호출 시 헤더로 넘긴 멱등키가 그대로 전달되는지 검증하도록 eq(testIdempotencyKey) 설정
            given(billingFacade.issueBillingKey(eq(memberId), eq(testIdempotencyKey), any(BillingIssueRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/billing/issue")
                            .header("Idempotency-Key", testIdempotencyKey) // 🔥 추가된 멱등성 헤더
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.billingInfoId").value(100L))
                    .andExpect(jsonPath("$.data.cardCompany").value("신한카드"))
                    .andExpect(jsonPath("$.message").value("빌링키 발급에 성공했습니다."));
        }

        @Test
        @DisplayName("실패: Idempotency-Key 헤더가 누락되면 400 Bad Request 에러가 발생한다.")
        void issueBillingKey_Fail_MissingIdempotencyHeader() throws Exception {
            // given
            BillingIssueRequest request = new BillingIssueRequest("test_auth_key", "test_customer_key");

            // when & then
            mockMvc.perform(post("/api/v1/billing/issue")
                            // .header("Idempotency-Key", ...) 의도적 누락
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isBadRequest()); // 필수 헤더 누락 시 400 발생
        }

        @Test
        @DisplayName("실패: authKey가 빈 값(Blank)이면 유효성 검증에 실패하여 400 에러가 발생한다.")
        void issueBillingKey_Fail_BlankAuthKey() throws Exception {
            // given
            BillingIssueRequest request = new BillingIssueRequest("", "test_customer_key");

            // when & then
            mockMvc.perform(post("/api/v1/billing/issue")
                            .header("Idempotency-Key", testIdempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("등록된 카드 목록 조회 API (GET /api/v1/billing)")
    class GetBillingKeysTest {

        @Test
        @DisplayName("성공: 회원이 등록한 카드 목록을 정상적으로 조회하고 200 OK를 반환한다.")
        void getBillingKeys_Success() throws Exception {
            // given
            List<BillingDetailsResponse> responses = List.of(
                    new BillingDetailsResponse(100L, "신한카드", "1234-****-****-****", LocalDateTime.now())
            );
            given(billingFacade.getBillingKeys(memberId)).willReturn(responses);

            // when & then
            mockMvc.perform(get("/api/v1/billing")
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].id").value(100L))
                    .andExpect(jsonPath("$.message").value("등록된 카드 목록 조회가 완료되었습니다."));
        }
    }

    @Nested
    @DisplayName("등록된 카드 삭제 및 해지 API (DELETE /api/v1/billing/{billingInfoId})")
    class DeleteBillingKeyTest {

        @Test
        @DisplayName("성공: 특정 빌링 정보 ID를 넘겨 안전하게 카드를 삭제하고 200 OK를 반환한다.")
        void deleteBillingKey_Success() throws Exception {
            // given
            Long billingInfoId = 100L;
            willDoNothing().given(billingFacade).deleteBillingKey(memberId, billingInfoId);

            // when & then
            mockMvc.perform(delete("/api/v1/billing/{billingInfoId}", billingInfoId)
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("카드가 안전하게 삭제 및 해지되었습니다."));
        }
    }
}