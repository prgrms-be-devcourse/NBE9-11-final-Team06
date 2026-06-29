package come.back.gotoday.payment.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.payment.subscription.dto.SubscriptionRequest;
import come.back.gotoday.payment.subscription.dto.SubscriptionResponse;
import come.back.gotoday.payment.subscription.service.SubscriptionFacade;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SubscriptionController 단위 테스트")
class SubscriptionControllerTest {

    private MockMvc mockMvc;
    private SubscriptionFacade subscriptionFacade;
    private ObjectMapper objectMapper;

    private CustomUserDetails testUserDetails;
    private final Long memberId = 1L;
    private final String testIdempotencyKey = "subscription-idempotency-key-2026";

    @BeforeEach
    void setUp() {
        subscriptionFacade = mock(SubscriptionFacade.class);
        objectMapper = new ObjectMapper();

        // 1. Mock Member 및 시큐리티 UserDetails 세팅
        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(memberId);
        given(mockMember.getEmail()).willReturn("user@gotoday.com");
        given(mockMember.getRole()).willReturn("USER");

        testUserDetails = new CustomUserDetails(mockMember);

        // 2. SecurityContextHolder에 강제 주입
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. 시큐리티 필터 없이 무겁지 않게 컨트롤러 딱 1개만 단독 빌드
        mockMvc = MockMvcBuilders.standaloneSetup(new SubscriptionController(subscriptionFacade))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("정기 구독 신청 API (POST /api/v1/subscriptions)")
    class StartSubscriptionTest {

        @Test
        @DisplayName("성공: 필수 멱등키와 유효한 바디가 주어지면 200 OK와 함께 결제 성공 메시지를 반환한다.")
        void startSubscription_Success() throws Exception {
            // given
            SubscriptionRequest request = new SubscriptionRequest(55L, 10L); // billingInfoId, planId
            SubscriptionResponse response = mock(SubscriptionResponse.class);

            given(subscriptionFacade.startSubscription(eq(memberId), any(SubscriptionRequest.class), eq(testIdempotencyKey)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/subscriptions")
                            .header("Idempotency-Key", testIdempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("정기 구독 신청 및 첫 달 결제가 완료되었습니다."));
        }

        @Test
        @DisplayName("실패: Idempotency-Key 헤더가 누락되면 스프링의 헤더 검증에 의해 400 에러가 발생한다.")
        void startSubscription_Fail_MissingHeader() throws Exception {
            // given
            SubscriptionRequest request = new SubscriptionRequest(55L, 10L);

            // when & then
            mockMvc.perform(post("/api/v1/subscriptions")
                            // .header("Idempotency-Key", ...) 의도적 헤더 누락
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isBadRequest());

            verify(subscriptionFacade, never()).startSubscription(any(), any(), any());
        }

        @Test
        @DisplayName("실패: 유효성 검증 위반 - 필수 바디 인자(null) 누락 시 400 에러가 발생한다.")
        void startSubscription_Fail_ValidationError() throws Exception {
            // given
            SubscriptionRequest invalidRequest = new SubscriptionRequest(null, null);

            // when & then
            mockMvc.perform(post("/api/v1/subscriptions")
                            .header("Idempotency-Key", testIdempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isBadRequest());

            verify(subscriptionFacade, never()).startSubscription(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("내 정기 구독 정보 조회 API (GET /api/v1/subscriptions/me)")
    class GetMySubscriptionTest {

        @Test
        @DisplayName("성공: 회원의 현재 활성화된 구독 정보를 조회하고 200 OK를 반환한다.")
        void getMySubscription_Success() throws Exception {
            // given
            SubscriptionResponse response = mock(SubscriptionResponse.class);
            given(subscriptionFacade.getMyActiveSubscription(memberId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/subscriptions/me")
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("현재 이용 중인 구독 정보 조회가 완료되었습니다."));
        }
    }

    @Nested
    @DisplayName("정기 구독 해지 API (DELETE /api/v1/subscriptions/{id})")
    class CancelSubscriptionTest {

        @Test
        @DisplayName("성공: 구독 ID를 넘겨 다음 결제일부터 청구되지 않도록 해지 예약 처리를 하고 200 OK를 반환한다.")
        void cancelSubscription_Success() throws Exception {
            // given
            Long subscriptionId = 999L;
            willDoNothing().given(subscriptionFacade).cancelSubscription(memberId, subscriptionId);

            // when & then
            mockMvc.perform(delete("/api/v1/subscriptions/{id}", subscriptionId)
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("정기 구독 해지 신청이 완료되었습니다. 다음 결제일부터 청구되지 않습니다."));
        }
    }
}