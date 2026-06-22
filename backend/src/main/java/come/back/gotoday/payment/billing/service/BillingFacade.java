package come.back.gotoday.payment.billing.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.payment.billing.dto.BillingDetailsResponse;
import come.back.gotoday.payment.billing.dto.BillingIssueRequest;
import come.back.gotoday.payment.billing.dto.BillingIssueResponse;
import come.back.gotoday.payment.billing.dto.TossBillingKeyResponse;
import come.back.gotoday.payment.billing.entity.BillingInfo;
import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import come.back.gotoday.payment.idempotency.service.IdempotencyManager;
import come.back.gotoday.external.toss.TossPaymentsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BillingFacade {

    private final TossPaymentsClient tossPaymentsClient;
    private final BillingService billingService;
    private final IdempotencyManager idempotencyManager;

    private final MemberRepository memberRepository;

    public BillingIssueResponse issueBillingKey(Long memberId, BillingIssueRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        String internalIdempotencyKey = String.format("BILLING_AUTH_%d_%s", memberId, request.authKey());

        // 1. 멱등성 락 획득
        IdempotencyKey idempotencyKeyEntity = idempotencyManager.getOrCreateLock(
                member, internalIdempotencyKey, "/v1/billing/authorizations/issue", request.toString()
        );

        // 2. 외부 API 호출 및 비즈니스 로직 수행
        try {
            // 토스 외부에 빌링키 발급 요청
            TossBillingKeyResponse tossResponse = tossPaymentsClient.requestBillingKey(
                    request.authKey(),
                    request.customerKey()
            );

            //  DB에 빌링키 정보 저장 및 회원 매핑
            BillingIssueResponse response = billingService.saveBillingInfo(memberId, tossResponse);

            // 최종 성공 시 SUCCESS 업데이트
            idempotencyManager.updateToSuccess(idempotencyKeyEntity, 200, response.toString());

            return response;

        } catch (Throwable throwable) {
            // 외부 토스 API 에러나 내부 비즈니스 로직 실패 시 명확하게 FAIL 상태로 변경 기록
            idempotencyManager.updateToFail(idempotencyKeyEntity, 500, throwable.getMessage());
            throw throwable;
        }
    }

    public List<BillingDetailsResponse> getBillingKeys(Long memberId) {
        return billingService.getBillingKeys(memberId);
    }

    public void deleteBillingKey(Long memberId, Long billingInfoId) {
        // 1. 본인 소유의 빌링 정보가 맞는지 조회 및 검증
        BillingInfo billingInfo = billingService.getBillingInfoValidated(billingInfoId, memberId);

        // 2. 토스페이먼츠 외부 연동 해지 API 호출
        // Converter에 의해 자동 복호화되어 꺼내지므로 평문 billingKey가 사용됩니다.
        tossPaymentsClient.deleteBillingKeyFromServer(billingInfo.getBillingKey());

        // 3. 외부 API 해지 성공 시, 로컬 DB 내부 데이터 삭제
        billingService.deleteBillingInfo(billingInfo);
    }
}