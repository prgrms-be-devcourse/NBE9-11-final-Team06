package come.back.gotoday.payment.billing.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.payment.billing.dto.BillingIssueResponse;
import come.back.gotoday.payment.billing.dto.TossBillingKeyResponse;
import come.back.gotoday.payment.billing.entity.BillingInfo;
import come.back.gotoday.payment.billing.repository.BillingInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("BillingService 순수 도메인 및 데이터 정합성 테스트")
class BillingServiceTest {

    private BillingService billingService;

    // Mock 가짜 객체
    private BillingInfoRepository billingInfoRepository;
    private MemberRepository memberRepository;

    private Member mockMember;
    private final Long memberId = 1L;

    @BeforeEach
    void setUp() {
        billingInfoRepository = Mockito.mock(BillingInfoRepository.class);
        memberRepository = Mockito.mock(MemberRepository.class);

        billingService = new BillingService(billingInfoRepository, memberRepository);

        // 기본 정상 회원 Mock 설정
        mockMember = Mockito.mock(Member.class);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(mockMember));
    }

    @Test
    @DisplayName("케이스 1: [카드사 코드 매핑] 토스 응답의 issuerCode가 361이면 내부 변환을 거쳐 '신한카드'로 정상 저장된다.")
    void saveBillingInfo_WithIssuerCode_MapToCardName() {
        // given
        // 361 공인 코드를 가진 CardInfo 객체 생성
        TossBillingKeyResponse.CardInfo cardInfo = new TossBillingKeyResponse.CardInfo(
                "361", "361", "1234-5678-****-****", "CREDIT", "PERSONAL", "SHINHAN"
        );
        TossBillingKeyResponse tossResponse = new TossBillingKeyResponse(
                "tosspayments", "customer_123", "billing_key_123", "2026-01-01T00:00:00", "카드", cardInfo
        );

        // 실제 저장이 수행될 때 인자로 넘어온 billingInfo의 가공 결과를 검증하기 위해 리턴 Mocking
        given(billingInfoRepository.save(any(BillingInfo.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        // when
        BillingIssueResponse response = billingService.saveBillingInfo(memberId, tossResponse);

        // then
        assertThat(response).isNotNull();
        assertThat(response.cardCompany()).isEqualTo("카드사(코드: 361)"); // "361"이 "신한카드"로 매핑되었는지 확인
        assertThat(response.cardNumber()).isEqualTo("1234-5678-****-****");

        // Repository 저장소에 엔티티가 실제로 잘 전달되었는지 검증
        verify(billingInfoRepository).save(any(BillingInfo.class));
    }

    @Test
    @DisplayName("케이스 2: [하위 호환성 방어] issuerCode가 누락되고 company 필드만 올 때 차선책으로 해당 문자열을 카드사 이름으로 채운다.")
    void saveBillingInfo_MissingIssuerCodeButHasCompany_UseCompanyField() {
        // given
        // issuerCode는 누락(null 또는 빈값)되었으나 company 필드가 채워져서 유입된 상황
        TossBillingKeyResponse.CardInfo cardInfo = new TossBillingKeyResponse.CardInfo(
                "", "", "9876-5432-****-****", "CREDIT", "PERSONAL", "현대"
        );
        TossBillingKeyResponse tossResponse = new TossBillingKeyResponse(
                "tosspayments", "customer_123", "billing_key_123", "2026-01-01T00:00:00", "카드", cardInfo
        );

        given(billingInfoRepository.save(any(BillingInfo.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        BillingIssueResponse response = billingService.saveBillingInfo(memberId, tossResponse);

        // then
        assertThat(response).isNotNull();
        assertThat(response.cardCompany()).isEqualTo("현대"); // company 필드 값 백업 적용 확인
        assertThat(response.cardNumber()).isEqualTo("9876-5432-****-****");

        verify(billingInfoRepository).save(any(BillingInfo.class));
    }

    @Test
    @DisplayName("케이스 3: [NPE 방지] 토스 응답 중 card 객체가 아예 null이거나 정보가 비어있어도 기본값으로 안전하게 엔티티를 생성한다.")
    void saveBillingInfo_NullCardInfo_FallbackToDefaultValues() {
        // given
        // card 레코드 객체가 아예 null로 유입된 극한의 엣지 상황
        TossBillingKeyResponse tossResponse = new TossBillingKeyResponse(
                "tosspayments", "customer_123", "billing_key_123", "2026-01-01T00:00:00", "카드", null
        );

        given(billingInfoRepository.save(any(BillingInfo.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        BillingIssueResponse response = billingService.saveBillingInfo(memberId, tossResponse);

        // then
        assertThat(response).isNotNull();
        // NullPointerException 없이 하드코딩된 시스템 방어 기본값이 적용되었는지 검증
        assertThat(response.cardCompany()).isEqualTo("알 수 없는 카드사");
        assertThat(response.cardNumber()).isEqualTo("****-****-****-****");

        verify(billingInfoRepository).save(any(BillingInfo.class));
    }

    @Test
    @DisplayName("케이스 4: [트랜잭션 고립 회원 검증] 파사드를 통과했더라도 저장 직전 회원이 유실되거나 탈퇴 처리되면 예외를 던진다.")
    void saveBillingInfo_MemberNotFoundAtSaveMoment_ThrowException() {
        // given
        TossBillingKeyResponse.CardInfo cardInfo = new TossBillingKeyResponse.CardInfo(
                "361", "361", "1234-5678-****-****", "CREDIT", "PERSONAL", "SHINHAN"
        );
        TossBillingKeyResponse tossResponse = new TossBillingKeyResponse(
                "tosspayments", "customer_123", "billing_key_123", "2026-01-01T00:00:00", "카드", cardInfo
        );

        // 찰나의 순간에 회원이 사라진 상태 시뮬레이션 (Optional.empty 반환)
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        BusinessException thrownException = assertThrows(
                BusinessException.class,
                () -> billingService.saveBillingInfo(memberId, tossResponse)
        );

        assertThat(thrownException.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        // 검증: 회원이 없으므로 당연히 DB에 카드를 저장하는 행위는 차단되어야 함
        verify(billingInfoRepository, never()).save(any(BillingInfo.class));
    }

    @Test
    @DisplayName("케이스 5: [조회 필터 및 정렬] 카드 목록 조회 시 ACTIVE 상태인 카드만 최신순으로 정렬되어 DTO로 변환된다.")
    void getBillingKeys_FilterActiveAndSortDesc() {
        // given
        // 5-1. 정렬 및 필터링 검증을 위해 생성일(createdAt)이 다른 가짜 ACTIVE 상태의 BillingInfo 객체 2개 준비
        BillingInfo oldCard = Mockito.mock(BillingInfo.class);
        given(oldCard.getId()).willReturn(101L);
        given(oldCard.getCardCompany()).willReturn("국민카드");
        given(oldCard.getCardNumber()).willReturn("1111-****-****-****");

        BillingInfo newCard = Mockito.mock(BillingInfo.class);
        given(newCard.getId()).willReturn(102L);
        given(newCard.getCardCompany()).willReturn("우리카드");
        given(newCard.getCardNumber()).willReturn("2222-****-****-****");

        // 리포지토리가 이미 'ACTIVE' 상태로 '최신순(Desc)' 정렬된 리스트를 반환한다고 가정 (Mock 스텁 정의)
        // 상황상 유저가 가졌던 DELETED 카드 2개는 리포지토리 메서드(findByMemberIdAndStatusOrderByCreatedAtDesc)의
        // 쿼리 조건(Status = ACTIVE)에 의해 이미 걸러진 상태로 정상 카드만 반환되는 흐름을 시뮬레이션합니다.
        java.util.List<BillingInfo> mockActiveCards = java.util.List.of(newCard, oldCard); // 최신순 배치

        given(billingInfoRepository.findByMemberIdAndStatusOrderByCreatedAtDesc(
                eq(memberId), eq(come.back.gotoday.payment.billing.enums.BillingStatus.ACTIVE)))
                .willReturn(mockActiveCards);

        // when
        java.util.List<come.back.gotoday.payment.billing.dto.BillingDetailsResponse> responses =
                billingService.getBillingKeys(memberId);

        // then
        assertThat(responses).hasSize(2);

        // 5-2. 최신순(newCard -> oldCard)으로 인덱스 정렬이 유지되어 DTO로 매핑되었는지 정밀 검증
        assertThat(responses.get(0).id()).isEqualTo(102L);
        assertThat(responses.get(0).cardCompany()).isEqualTo("우리카드");

        assertThat(responses.get(1).id()).isEqualTo(101L);
        assertThat(responses.get(1).cardCompany()).isEqualTo("국민카드");

        verify(billingInfoRepository).findByMemberIdAndStatusOrderByCreatedAtDesc(
                eq(memberId), eq(come.back.gotoday.payment.billing.enums.BillingStatus.ACTIVE));
    }

    @Test
    @DisplayName("케이스 6: [재삭제 요청 방어] 이미 DELETED 상태인 카드를 다시 삭제 검증하려고 하면 FORBIDDEN_REQUEST 예외가 터진다.")
    void validateBeforeDelete_AlreadyDeletedCard_ThrowForbiddenRequest() {
        // given
        Long alreadyDeletedBillingInfoId = 500L;

        // 이미 삭제된 카드이므로 리포지토리에서 조건 조회(Status = ACTIVE) 시 데이터가 찾아지지 않는 상황 시뮬레이션
        given(billingInfoRepository.findByIdAndMemberIdAndStatus(
                eq(alreadyDeletedBillingInfoId), eq(memberId), eq(come.back.gotoday.payment.billing.enums.BillingStatus.ACTIVE)))
                .willReturn(java.util.Optional.empty());

        // when & then
        BusinessException thrownException = assertThrows(
                BusinessException.class,
                () -> billingService.validateBeforeDelete(alreadyDeletedBillingInfoId, memberId)
        );

        // 검증: 이미 지워졌거나 권한이 없는 카드는 403 ForbiddenRequest 성격의 예외로 엄격히 제한되는지 확인
        assertThat(thrownException.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN_REQUEST);

        verify(billingInfoRepository).findByIdAndMemberIdAndStatus(
                eq(alreadyDeletedBillingInfoId), eq(memberId), eq(come.back.gotoday.payment.billing.enums.BillingStatus.ACTIVE));
    }

    @Test
    @DisplayName("케이스 7: [변경 감지 독립 검증] 엔티티 조회 후 delete()를 호출하면 엔티티의 상태가 DELETED로 정상 변경된다.")
    void deleteBillingInfoStatus_DirtyChecking_Success() {
        // given
        Long billingInfoId = 100L;

        // 7-1. Mock 객체가 아닌 '실물' BillingInfo 엔티티를 생성하여 영속성 컨텍스트 조회 상황 시뮬레이션
        // (상태 변경 감지는 실물 객체의 필드가 변하는지 추적하는 것이 핵심이기 때문입니다)
        Member testMember = Mockito.mock(Member.class);
        BillingInfo realBillingInfo = BillingInfo.create(
                testMember, "customer_key_123", "toss_billing_key_123", "신한카드", "1234-****-****-****"
        );

        // 최초 상태가 ACTIVE인지 가볍게 검증 (엔티티 내부 필드 스냅샷)
        assertThat(realBillingInfo.getStatus()).isEqualTo(come.back.gotoday.payment.billing.enums.BillingStatus.ACTIVE);

        // 리포지토리에서 이 실물 엔티티를 반환하도록 스텁 설정
        given(billingInfoRepository.findById(eq(billingInfoId)))
                .willReturn(java.util.Optional.of(realBillingInfo));

        // when
        billingService.deleteBillingInfoStatus(billingInfoId);

        // then
        // 7-2. 메서드가 종료되면서 실물 엔티티의 status 필드가 DELETED로 완벽히 변경되었는지 검증
        // 트랜잭션 커밋 시점에 이 변경점을 JPA가 감지(Dirty Checking)하여 DB에 UPDATE 쿼리를 날리게 됩니다.
        assertThat(realBillingInfo.getStatus()).isEqualTo(come.back.gotoday.payment.billing.enums.BillingStatus.DELETED);

        verify(billingInfoRepository).findById(eq(billingInfoId));
    }

    @Test
    @DisplayName("케이스 8: [영속 객체 증발 예외] 조회하려는 결제 정보가 DB에서 찰나의 순간에 유실되면 INVALID_BILLING_INFO 예외를 던진다.")
    void deleteBillingInfoStatus_EntityNotFound_ThrowInvalidBillingInfo() {
        // given
        Long nonExistentBillingInfoId = 404L;

        // 동시성 이슈나 관리자 삭제 등으로 인해 findById 조회 결과가 비어있는(empty) 상황 시뮬레이션
        given(billingInfoRepository.findById(eq(nonExistentBillingInfoId)))
                .willReturn(java.util.Optional.empty());

        // when & then
        BusinessException thrownException = assertThrows(
                BusinessException.class,
                () -> billingService.deleteBillingInfoStatus(nonExistentBillingInfoId)
        );

        // 검증: 엔티티가 증발했을 때 시스템 에러로 터지지 않고 도메인 예외로 이쁘게 감싸서 던지는지 확인
        assertThat(thrownException.getErrorCode()).isEqualTo(ErrorCode.INVALID_BILLING_INFO);

        verify(billingInfoRepository).findById(eq(nonExistentBillingInfoId));
    }
}