package come.back.gotoday.payment.billing.service;

import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.billing.dto.BillingIssueRequest;
import come.back.gotoday.payment.billing.dto.BillingIssueResponse;
import come.back.gotoday.payment.billing.dto.TossBillingKeyResponse;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.payment.billing.entity.BillingInfo;
import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import come.back.gotoday.payment.idempotency.enums.IdempotencyStatus;
import come.back.gotoday.payment.idempotency.service.IdempotencyManager;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("BillingFacade 멱등성 성공 및 구출 시나리오 테스트")
class BillingFacadeTest {

    private BillingFacade billingFacade;

    // 가짜 협력 객체들 (Mock)
    private TossPaymentsClient tossPaymentsClient;
    private BillingService billingService;
    private IdempotencyManager idempotencyManager;
    private ObjectMapper objectMapper;
    private MemberRepository memberRepository;
    private SubscriptionRepository subscriptionRepository;

    private Member mockMember;
    private final Long memberId = 1L;
    private final String idempotencyKey = "test-idempotency-key-1234";
    private final String requestPath = "/v1/billing/authorizations/issue";

    @BeforeEach
    void setUp() {
        tossPaymentsClient = Mockito.mock(TossPaymentsClient.class);
        billingService = Mockito.mock(BillingService.class);
        idempotencyManager = Mockito.mock(IdempotencyManager.class);
        objectMapper = new ObjectMapper(); // 순수 ObjectMapper 사용 또는 Mock 필요시 Mocking
        memberRepository = Mockito.mock(MemberRepository.class);
        subscriptionRepository = Mockito.mock(SubscriptionRepository.class);

        billingFacade = new BillingFacade(
                tossPaymentsClient,
                billingService,
                idempotencyManager,
                objectMapper,
                memberRepository,
                subscriptionRepository
        );

        // 기본 회원 Mock 설정
        mockMember = Mockito.mock(Member.class);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(mockMember));
    }

    @Test
    @DisplayName("케이스 1: [최초 요청 완전히 성공] 새로운 멱등키로 진입하여 토스 및 DB 저장에 완전히 성공한다.")
    void issueBillingKey_FirstRequest_Success() throws Exception {
        // given
        BillingIssueRequest request = new BillingIssueRequest("authKey_123", "customerKey_123");

        // 1-1. 최초 요청이므로 PROCESSING 상태인 가짜 IdempotencyKey 엔티티 준비
        IdempotencyKey mockIdempotencyEntity = Mockito.mock(IdempotencyKey.class);
        given(mockIdempotencyEntity.getStatus()).willReturn(IdempotencyStatus.PROCESSING);

        given(idempotencyManager.getOrCreateLock(eq(mockMember), eq(idempotencyKey), eq(requestPath), any()))
                .willReturn(mockIdempotencyEntity);

        // 1-2. 토스 외부 API 결과 Mocking
        TossBillingKeyResponse.CardInfo cardInfo = new TossBillingKeyResponse.CardInfo(
                "361", "361", "1234-5678-****-****", "CREDIT", "PERSONAL", "신한"
        );
        TossBillingKeyResponse mockTossResponse = new TossBillingKeyResponse(
                "tosspayments", "customerKey_123", "billingKey_123", "2026-01-01T00:00:00", "카드", cardInfo
        );
        given(tossPaymentsClient.requestBillingKey(idempotencyKey, request.authKey(), request.customerKey()))
                .willReturn(mockTossResponse);

        // 1-3. 내부 서비스 저장 결과 Mocking
        BillingIssueResponse expectedResponse = new BillingIssueResponse(100L, "신한카드", "1234-****-****-****");
        given(billingService.saveBillingInfo(memberId, mockTossResponse))
                .willReturn(expectedResponse);

        // when
        BillingIssueResponse actualResponse = billingFacade.issueBillingKey(memberId, idempotencyKey, request);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.billingInfoId()).isEqualTo(100L);
        assertThat(actualResponse.cardCompany()).isEqualTo("신한카드");

        // 검증: 락을 잡고, 외부 API를 호출한 뒤, 성공 상태로 업데이트했는가?
        verify(idempotencyManager).getOrCreateLock(eq(mockMember), eq(idempotencyKey), eq(requestPath), any());
        verify(tossPaymentsClient).requestBillingKey(idempotencyKey, request.authKey(), request.customerKey());
        verify(billingService).saveBillingInfo(memberId, mockTossResponse);
        verify(idempotencyManager).updateToSuccess(eq(mockIdempotencyEntity), eq(200), any(String.class));
    }

    @Test
    @DisplayName("케이스 2: [이미 성공한 요청의 중복 진입] 토스 API와 DB 저장을 호출하지 않고 저장된 바디를 Early Return한다.")
    void issueBillingKey_AlreadySuccessRequest_EarlyReturn() throws Exception {
        // given
        BillingIssueRequest request = new BillingIssueRequest("authKey_123", "customerKey_123");
        String savedJsonBody = "{\"billingInfoId\":100,\"cardCompany\":\"신한카드\",\"cardNumber\":\"1234-****-****-****\"}";

        // 2-1. 이미 SUCCESS 상태이고 내부에 기존 응답 결과가 기록된 가짜 IdempotencyKey 준비
        IdempotencyKey mockIdempotencyEntity = Mockito.mock(IdempotencyKey.class);
        given(mockIdempotencyEntity.getStatus()).willReturn(IdempotencyStatus.SUCCESS);
        given(mockIdempotencyEntity.getResponseBody()).willReturn(savedJsonBody);

        given(idempotencyManager.getOrCreateLock(eq(mockMember), eq(idempotencyKey), eq(requestPath), any()))
                .willReturn(mockIdempotencyEntity);

        // when
        BillingIssueResponse actualResponse = billingFacade.issueBillingKey(memberId, idempotencyKey, request);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.billingInfoId()).isEqualTo(100L);
        assertThat(actualResponse.cardCompany()).isEqualTo("신한카드");

        // 검증: 외부 API 통신 및 내부 DB 서비스 저장이 '절대' 호출되지 않았어야 함 (원천 차단 및 조기 리턴)
        verify(tossPaymentsClient, never()).requestBillingKey(any(), any(), any());
        verify(billingService, never()).saveBillingInfo(any(), any());
        verify(idempotencyManager, never()).updateToSuccess(any(), any(Integer.class), any());
    }

    @Test
    @DisplayName("케이스 3: [TIMEOUT 상태인 멱등키의 구출] TIMEOUT 상태였던 키가 재진입하면 락을 다시 획득하고 최종 성공 처리한다.")
    void issueBillingKey_TimeoutRequest_RescueAndSuccess() throws Exception {
        // given
        BillingIssueRequest request = new BillingIssueRequest("authKey_123", "customerKey_123");

        // 3-1. 1차 관리자(IdempotencyManager)를 호출했을 때, 기존에 TIMEOUT이었던 키는 내부 로직(startProcessingAgain)에 의해
        // 다시 PROCESSING 상태 상태 전이를 마친 채 반환됩니다. (페사드 입장에서는 상태가 PROCESSING으로 들어옴)
        IdempotencyKey rescuedIdempotencyEntity = Mockito.mock(IdempotencyKey.class);
        given(rescuedIdempotencyEntity.getStatus()).willReturn(IdempotencyStatus.PROCESSING); // 구출되어 다시 진입 가능해진 상태

        given(idempotencyManager.getOrCreateLock(eq(mockMember), eq(idempotencyKey), eq(requestPath), any()))
                .willReturn(rescuedIdempotencyEntity);

        // 3-2. 재시도 시에는 토스 외부 통신 성공 시뮬레이션
        TossBillingKeyResponse.CardInfo cardInfo = new TossBillingKeyResponse.CardInfo(
                "365", "365", "5678-1234-****-****", "CREDIT", "PERSONAL", "삼성"
        );
        TossBillingKeyResponse mockTossResponse = new TossBillingKeyResponse(
                "tosspayments", "customerKey_123", "billingKey_123", "2026-01-01T00:00:00", "카드", cardInfo
        );
        given(tossPaymentsClient.requestBillingKey(idempotencyKey, request.authKey(), request.customerKey()))
                .willReturn(mockTossResponse);

        // 3-3. 서비스 저장 성공 시뮬레이션
        BillingIssueResponse expectedResponse = new BillingIssueResponse(200L, "현대카드", "5678-****-****-****");
        given(billingService.saveBillingInfo(memberId, mockTossResponse))
                .willReturn(expectedResponse);

        // when
        BillingIssueResponse actualResponse = billingFacade.issueBillingKey(memberId, idempotencyKey, request);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.billingInfoId()).isEqualTo(200L);
        assertThat(actualResponse.cardCompany()).isEqualTo("현대카드");

        // 검증: 락 획득 시도를 하고 타임아웃 키가 구출되어 비즈니스 로직(토스 호출, 저장, 최종 성공 저장)이 끊김 없이 완수되었는지 검증
        verify(idempotencyManager).getOrCreateLock(eq(mockMember), eq(idempotencyKey), eq(requestPath), any());
        verify(tossPaymentsClient).requestBillingKey(idempotencyKey, request.authKey(), request.customerKey());
        verify(billingService).saveBillingInfo(memberId, mockTossResponse);
        verify(idempotencyManager).updateToSuccess(eq(rescuedIdempotencyEntity), eq(200), any(String.class));
    }

    @Test
    @DisplayName("케이스 4: [찰나의 연타 중복 진입 차단] 1차 요청이 처리 중(PROCESSING)일 때 동일 키로 진입하면 예외를 던지며 토스 호출을 차단한다.")
    void issueBillingKey_ProcessingRequest_ConcurrencyBlock() throws Exception {
        // given
        BillingIssueRequest request = new BillingIssueRequest("authKey_123", "customerKey_123");

        // 4-1. IdempotencyManager가 동일 키 연타를 감지하여 ALREADY_PROCESSED_PAYMENT 예외를 던지는 상황 시뮬레이션
        given(idempotencyManager.getOrCreateLock(eq(mockMember), eq(idempotencyKey), eq(requestPath), any()))
                .willThrow(new BusinessException(
                       ErrorCode.ALREADY_PROCESSED_PAYMENT
                ));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> billingFacade.issueBillingKey(memberId, idempotencyKey, request),
                "이미 처리 중이거나 완료된 결제 요청이라는 예외가 발생해야 합니다."
        );

        // 검증: 매니저에서 즉시 차단당했으므로 외부 토스 API나 내부 저장 서비스는 '절대' 호출되지 않아야 함
        verify(tossPaymentsClient, never()).requestBillingKey(any(), any(), any());
        verify(billingService, never()).saveBillingInfo(any(), any());
    }

    @Test
    @DisplayName("케이스 5: [이미 영구 실패한 멱등키의 재진입 차단] FAILED로 마감된 키가 들어오면 예외를 발생시키며 흐름을 원천 차단한다.")
    void issueBillingKey_AlreadyFailedRequest_Block() throws Exception {
        // given
        BillingIssueRequest request = new BillingIssueRequest("authKey_123", "customerKey_123");

        // 5-1. 영구 실패(FAILED)한 키의 재유입은 정책상 차단 대상이므로 매니저가 ALREADY_PROCESSED_PAYMENT를 던짐
        given(idempotencyManager.getOrCreateLock(eq(mockMember), eq(idempotencyKey), eq(requestPath), any()))
                .willThrow(new BusinessException(
                       ErrorCode.ALREADY_PROCESSED_PAYMENT
                ));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(
               BusinessException.class,
                () -> billingFacade.issueBillingKey(memberId, idempotencyKey, request),
                "실패로 완결된 키의 재진입은 차단되어야 합니다."
        );

        // 검증: 진입 시점에 튕겨 나갔으므로 외부 API 호출 등의 후속 조치는 절대 발생하지 않음
        verify(tossPaymentsClient, never()).requestBillingKey(any(), any(), any());
        verify(billingService, never()).saveBillingInfo(any(), any());
    }

    @Test
    @DisplayName("케이스 6: [토스 API 호출 중 최종 타임아웃 발생 시 TIMEOUT 전이] 최종 타임아웃 예외 발생 시 상태를 TIMEOUT으로 변경하고 예외를 전파한다.")
    void issueBillingKey_TossApiTimeout_UpdateToTimeout() throws Exception {
        // given
        BillingIssueRequest request = new BillingIssueRequest("authKey_123", "customerKey_123");

        // 6-1. 정상 진입 상태인 PROCESSING 락 획득 시뮬레이션
        IdempotencyKey mockIdempotencyEntity = Mockito.mock(IdempotencyKey.class);
        given(mockIdempotencyEntity.getStatus()).willReturn(IdempotencyStatus.PROCESSING);
        given(idempotencyManager.getOrCreateLock(eq(mockMember), eq(idempotencyKey), eq(requestPath), any()))
                .willReturn(mockIdempotencyEntity);

        // 6-2. 토스 클라이언트에서 최종 네트워크 타임아웃(NETWORK_ERROR_FINAL_FAILED) 발생 시뮬레이션
        BusinessException timeoutException =
                new BusinessException(ErrorCode.NETWORK_ERROR_FINAL_FAILED);

        given(tossPaymentsClient.requestBillingKey(idempotencyKey, request.authKey(), request.customerKey()))
                .willThrow(timeoutException);

        // when & then
        BusinessException thrownException = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> billingFacade.issueBillingKey(memberId, idempotencyKey, request)
        );

        assertThat(thrownException.getErrorCode()).isEqualTo(ErrorCode.NETWORK_ERROR_FINAL_FAILED);

        // 검증: 페사드가 예외를 잡아서 구출 가능한 'TIMEOUT' 상태로 안전하게 업데이트를 쳤는지 확인
        verify(idempotencyManager).updateToTimeout(eq(mockIdempotencyEntity), eq(504), any());
        // 내부 DB 저장은 호출되지 않았어야 함
        verify(billingService, never()).saveBillingInfo(any(), any());
    }

    @Test
    @DisplayName("케이스 7: [토스 API 호출 중 일반 카드사 에러 발생 시 FAILED 전이] 한도초과/번호오류 등 일반 비즈니스 에러 시 FAILED로 영구 종결한다.")
    void issueBillingKey_TossApiBusinessError_UpdateToFail() throws Exception {
        // given
        BillingIssueRequest request = new BillingIssueRequest("authKey_123", "customerKey_123");

        IdempotencyKey mockIdempotencyEntity = Mockito.mock(IdempotencyKey.class);
        given(mockIdempotencyEntity.getStatus()).willReturn(IdempotencyStatus.PROCESSING);
        given(idempotencyManager.getOrCreateLock(eq(mockMember), eq(idempotencyKey), eq(requestPath), any()))
                .willReturn(mockIdempotencyEntity);

        // 7-1. 카드사 한도 초과나 번호 오류 등 일반 결제 비즈니스 예외(예: INVALID_CARD_NUMBER라 가정) 발생 시뮬레이션
        // 여기서는 NETWORK_ERROR_FINAL_FAILED가 아닌 일반 BusinessException
        BusinessException cardException =
                new BusinessException(ErrorCode.INVALID_CARD_NUMBER); // 혹은 시스템에 맞는 일반 에러코드 사용

        given(tossPaymentsClient.requestBillingKey(idempotencyKey, request.authKey(), request.customerKey()))
                .willThrow(cardException);

        // when & then
        BusinessException thrownException = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> billingFacade.issueBillingKey(memberId, idempotencyKey, request)
        );

        // 검증: 재시도가 불가능하도록 'updateToFail' 영구 실패 처리가 일어났는지 확인
        verify(idempotencyManager).updateToFail(eq(mockIdempotencyEntity), eq(400), any());
        verify(billingService, never()).saveBillingInfo(any(), any());
    }

    @Test
    @DisplayName("케이스 8: [토스 외부 API는 성공했으나 내부 DB 저장 실패] 외부 통신은 성공했으나 서비스 저장 중 예외가 터지면 FAILED(500) 처리한다.")
    void issueBillingKey_ExternalSuccessButDbSaveFail_UpdateToFail() throws Exception {
        // given
        BillingIssueRequest request = new BillingIssueRequest("authKey_123", "customerKey_123");

        IdempotencyKey mockIdempotencyEntity = Mockito.mock(IdempotencyKey.class);
        given(mockIdempotencyEntity.getStatus()).willReturn(IdempotencyStatus.PROCESSING);
        given(idempotencyManager.getOrCreateLock(eq(mockMember), eq(idempotencyKey), eq(requestPath), any()))
                .willReturn(mockIdempotencyEntity);

        // 8-1. 토스 API는 정상 응답을 반환함
        TossBillingKeyResponse.CardInfo cardInfo = new TossBillingKeyResponse.CardInfo(
                "361", "361", "1234-5678-****-****", "CREDIT", "PERSONAL", "신한"
        );
        TossBillingKeyResponse mockTossResponse = new TossBillingKeyResponse(
                "tosspayments", "customerKey_123", "billingKey_123", "2026-01-01T00:00:00", "카드", cardInfo
        );
        given(tossPaymentsClient.requestBillingKey(idempotencyKey, request.authKey(), request.customerKey()))
                .willReturn(mockTossResponse);

        // 8-2. 그러나 내부 서비스 로직( saveBillingInfo ) 트랜잭션 수행 중 원인 모를 런타임 예외(DB 다운 등) 발생 시뮬레이션
        given(billingService.saveBillingInfo(memberId, mockTossResponse))
                .willThrow(new RuntimeException("데이터베이스 커넥션 타임아웃 장애"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> billingFacade.issueBillingKey(memberId, idempotencyKey, request)
        );

        // 검증: 알 수 없는 일반 Exception 이므로 최하단 catch 블록에서 안전하게 FAILED(500) 마킹을 쳤는지 검증
        verify(idempotencyManager).updateToFail(eq(mockIdempotencyEntity), eq(500), any());
        // 최종 성공 로직은 절대 실행되지 않았어야 함
        verify(idempotencyManager, never()).updateToSuccess(any(), any(Integer.class), any());
    }

    @Test
    @DisplayName("케이스 9: [멱등성 응답 파싱 에러 방어] SUCCESS 상태의 응답값 파싱(Json 자르기) 실패 시 INTERNAL_SERVER_ERROR를 던진다.")
    void issueBillingKey_SuccessResponseParsingFail_ThrowInternalServerError() throws Exception {
        // given
        BillingIssueRequest request = new BillingIssueRequest("authKey_123", "customerKey_123");
        String brokenJson = "{\"billingInfoId\": 깨진 데이터 포맷 {{";

        IdempotencyKey mockIdempotencyEntity = Mockito.mock(IdempotencyKey.class);
        given(mockIdempotencyEntity.getStatus()).willReturn(IdempotencyStatus.SUCCESS);
        given(mockIdempotencyEntity.getResponseBody()).willReturn(brokenJson); // 의도적으로 손상된 JSON 주입

        given(idempotencyManager.getOrCreateLock(eq(mockMember), eq(idempotencyKey), eq(requestPath), any()))
                .willReturn(mockIdempotencyEntity);

        // when & then
        BusinessException thrownException = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> billingFacade.issueBillingKey(memberId, idempotencyKey, request)
        );

        // 검증: 파싱 실패 시 INTERNAL_SERVER_ERROR 에러 코드로 환원되어 안전하게 전파되는가
        assertThat(thrownException.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("케이스 10: [정상 삭제 성공] 본인 확인 및 활성 구독 체크를 정상 통과하여 토스 서버 해지와 로컬 DB 삭제를 완수한다.")
    void deleteBillingKey_Success() throws Exception {
        // given
        Long billingInfoId = 100L;

        // 10-1. 본인 소유의 가짜 BillingInfo 준비 및 토스 빌링키 반환 설정
        BillingInfo mockBillingInfo = Mockito.mock(BillingInfo.class);
        given(mockBillingInfo.getBillingKey()).willReturn("toss_billing_key_sample_123");

        given(billingService.validateBeforeDelete(billingInfoId, memberId))
                .willReturn(mockBillingInfo);

        // 10-2. 활성 구독이 존재하지 않음 (false) 설정
        given(subscriptionRepository.existsByBillingInfoIdAndStatus(
                billingInfoId, SubscriptionStatus.ACTIVE))
                .willReturn(false);

        // 10-3. 토스 외부 API 해지 및 서비스 상태변경 정상 작동 모킹
        Mockito.doNothing().when(tossPaymentsClient).deleteBillingKeyFromServer("toss_billing_key_sample_123");
        Mockito.doNothing().when(billingService).deleteBillingInfoStatus(billingInfoId);

        // when
        billingFacade.deleteBillingKey(memberId, billingInfoId);

        // then & 검증
        verify(billingService).validateBeforeDelete(billingInfoId, memberId);
        verify(subscriptionRepository).existsByBillingInfoIdAndStatus(billingInfoId, SubscriptionStatus.ACTIVE);
        verify(tossPaymentsClient).deleteBillingKeyFromServer("toss_billing_key_sample_123");
        verify(billingService).deleteBillingInfoStatus(billingInfoId);
    }

    @Test
    @DisplayName("케이스 11: [타인 카드 삭제 시도 차단] 검증 단계에서 FORBIDDEN_REQUEST 예외가 터지면 삭제가 원천 차단된다.")
    void deleteBillingKey_NotOwner_ForbiddenRequest() throws Exception {
        // given
        Long billingInfoId = 999L; // 타인의 카드 ID 혹은 존재하지 않는 카드

        // 11-1. 서비스 검증 단계에서 권한 예외를 뿜어내도록 스텁 설정
        given(billingService.validateBeforeDelete(billingInfoId, memberId))
                .willThrow(new BusinessException(
                        ErrorCode.FORBIDDEN_REQUEST
                ));

        // when & then
        BusinessException thrownException = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> billingFacade.deleteBillingKey(memberId, billingInfoId)
        );

        assertThat(thrownException.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN_REQUEST);

        // 검증: 첫 줄에서 튕겼으므로 구독 레포 조회, 토스 API, 상태 변경 등의 후속작업은 일절 발생 안 함
        verify(subscriptionRepository, never()).existsByBillingInfoIdAndStatus(any(), any());
        verify(tossPaymentsClient, never()).deleteBillingKeyFromServer(any());
        verify(billingService, never()).deleteBillingInfoStatus(any());
    }

    @Test
    @DisplayName("케이스 12: [활성 구독 카드의 삭제 시도 차단] 해당 카드로 매달 나가는 구독이 걸려있으면 예외를 발생시킨다.")
    void deleteBillingKey_ActiveSubscriptionExists_Block() throws Exception {
        // given
        Long billingInfoId = 100L;
        BillingInfo mockBillingInfo = Mockito.mock(BillingInfo.class);

        given(billingService.validateBeforeDelete(billingInfoId, memberId))
                .willReturn(mockBillingInfo);

        // 12-1. 해당 카드에 연결된 활성(ACTIVE) 상태의 구독이 존재함(true)을 반환하도록 설정
        given(subscriptionRepository.existsByBillingInfoIdAndStatus(
                billingInfoId, SubscriptionStatus.ACTIVE))
                .willReturn(true);

        // when & then
        BusinessException thrownException = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> billingFacade.deleteBillingKey(memberId, billingInfoId)
        );

        assertThat(thrownException.getErrorCode()).isEqualTo(ErrorCode.CANNOT_DELETE_ACTIVE_CARD);

        // 검증: 비즈니스 정책 위반이므로 토스 서버에 전화를 걸어 카드를 지우는 치명적인 행위는 '절대' 일어나지 않음
        verify(tossPaymentsClient, never()).deleteBillingKeyFromServer(any());
        verify(billingService, never()).deleteBillingInfoStatus(any());
    }

    @Test
    @DisplayName("케이스 13: [데이터 불일치 대응] 토스 서버 삭제는 성공했으나 내부 DB 상태 변경 실패 시 로그를 남기고 서버에러를 유발한다.")
    void deleteBillingKey_TossSuccessButLocalDbFail_DataMismatchHandling() throws Exception {
        // given
        Long billingInfoId = 100L;
        BillingInfo mockBillingInfo = Mockito.mock(BillingInfo.class);
        given(mockBillingInfo.getBillingKey()).willReturn("toss_billing_key_critical_456");

        given(billingService.validateBeforeDelete(billingInfoId, memberId))
                .willReturn(mockBillingInfo);

        given(subscriptionRepository.existsByBillingInfoIdAndStatus(
                billingInfoId, SubscriptionStatus.ACTIVE))
                .willReturn(false);

        // 13-1. 토스 서버 삭제는 문제없이 성공 처리됨 (이미 토스 측 빌링키 파괴 완료)
        Mockito.doNothing().when(tossPaymentsClient).deleteBillingKeyFromServer("toss_billing_key_critical_456");

        // 13-2. 그러나 바로 다음 줄인 우리 로컬 DB 상태 변경 메서드 수행 도중 에러(일시적 Lock, 인프라 다운 등)가 발생함
        Mockito.doThrow(new RuntimeException("DB 영속성 더티체킹 반영 실패 (데드락 장애)"))
                .when(billingService).deleteBillingInfoStatus(eq(billingInfoId));

        // when & then
        BusinessException thrownException = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> billingFacade.deleteBillingKey(memberId, billingInfoId)
        );

        // 검증: 사후 구출이 필요한 치명적인 불일치 상황이므로 INTERNAL_SERVER_ERROR 가 터졌는지 확인
        assertThat(thrownException.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);

        // 검증: 외부 토스 API는 호출되었지만 로컬 처리가 터진 순서가 명확한지 재확인
        verify(tossPaymentsClient).deleteBillingKeyFromServer("toss_billing_key_critical_456");
    }
}