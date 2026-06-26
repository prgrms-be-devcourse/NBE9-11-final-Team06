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
import come.back.gotoday.payment.billing.enums.BillingStatus;
import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import come.back.gotoday.payment.idempotency.enums.IdempotencyStatus;
import come.back.gotoday.payment.idempotency.service.IdempotencyManager;
import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingFacade {

    private final TossPaymentsClient tossPaymentsClient;
    private final BillingService billingService;
    private final IdempotencyManager idempotencyManager;
    private final ObjectMapper objectMapper;

    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;

    public BillingIssueResponse issueBillingKey(Long memberId, String idempotencyKey, BillingIssueRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 1. 멱등성 락 획득
        IdempotencyKey idempotencyKeyEntity = idempotencyManager.getOrCreateLock(
                member, idempotencyKey, "/v1/billing/authorizations/issue", request.toString()
        );

        if (idempotencyKeyEntity.getStatus() == IdempotencyStatus.SUCCESS) {
            try {
                return objectMapper.readValue(idempotencyKeyEntity.getResponseBody(), BillingIssueResponse.class);
            } catch (Exception e) {
                log.error("멱등성 응답 파싱 실패", e);
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }
        // 2. 외부 API 호출 및 비즈니스 로직 수행
        try {
            // 토스 외부에 빌링키 발급 요청
            TossBillingKeyResponse tossResponse = tossPaymentsClient.requestBillingKey(
                    idempotencyKey,
                    request.authKey(),
                    request.customerKey()
            );

            //  DB에 빌링키 정보 저장 및 회원 매핑
            BillingIssueResponse response = billingService.saveBillingInfo(memberId, tossResponse);

            // 최종 성공 시 SUCCESS 업데이트
            String responseJson;
            try {
                responseJson = objectMapper.writeValueAsString(response);
            } catch (Exception e) {
                log.error("멱등성 응답 직렬화 실패", e);
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            idempotencyManager.updateToSuccess(idempotencyKeyEntity, 200, responseJson);

            return response;

        } catch (BusinessException e) {
            // 클라이언트(@Recover)가 던진 최종 타임아웃 예외인 경우
            if (e.getErrorCode() == ErrorCode.NETWORK_ERROR_FINAL_FAILED) {
                // 동일 키로 다시 들어올 수 있게 TIMEOUT 상태로 기록!
                idempotencyManager.updateToTimeout(idempotencyKeyEntity, 504, e.getMessage());
            } else {
                // 카드 유효기간 만료, 번호 오류 등 일반 비즈니스 에러인 경우 영구 실패(FAILED)
                idempotencyManager.updateToFail(idempotencyKeyEntity, 400, e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            // 그 외 알 수 없는 시스템 예외는 안전하게 FAILED 처리
            idempotencyManager.updateToFail(idempotencyKeyEntity, 500, e.getMessage());
            throw e;
        }
    }

    public List<BillingDetailsResponse> getBillingKeys(Long memberId) {
        return billingService.getBillingKeys(memberId);
    }

    public void deleteBillingKey(Long memberId, Long billingInfoId) {
        BillingInfo billingInfo = billingService.validateBeforeDelete(billingInfoId, memberId);
        if (subscriptionRepository.existsByBillingInfoIdAndStatus(billingInfoId, SubscriptionStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_ACTIVE_CARD);
        }

        tossPaymentsClient.deleteBillingKeyFromServer(billingInfo.getBillingKey());


        try {
            billingService.deleteBillingInfoStatus(billingInfoId);
        } catch (Exception e) {
            log.error("=== 데이터 불일치 발생 ===");
            log.error("토스 서버의 빌링키는 삭제되었으나, 로컬 DB 상태 변경에 실패했습니다. BillingInfo ID: {}", billingInfoId, e);

            // 얼럿 시스템(슬랙 알림 등) 연동 구역 혹은 강제 상태 변경 재시도 로직 필요
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}