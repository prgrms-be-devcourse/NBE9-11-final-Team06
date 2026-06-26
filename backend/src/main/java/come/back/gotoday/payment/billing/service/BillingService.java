package come.back.gotoday.payment.billing.service;

import come.back.gotoday.external.toss.enums.TossCardCode;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.payment.billing.dto.BillingDetailsResponse;
import come.back.gotoday.payment.billing.dto.BillingIssueResponse;
import come.back.gotoday.payment.billing.dto.TossBillingKeyResponse;
import come.back.gotoday.payment.billing.entity.BillingInfo;
import come.back.gotoday.payment.billing.enums.BillingStatus;
import come.back.gotoday.payment.billing.repository.BillingInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingInfoRepository billingInfoRepository;
    private final MemberRepository memberRepository;

    /**
     * 외부 API 호출이 완전히 끝난 후, 순수하게 DB 작업만 수행하는 트랜잭션 서브루틴
     */
    @Transactional
    public BillingIssueResponse saveBillingInfo(Long memberId, TossBillingKeyResponse tossResponse) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        // 2. 안전하게 카드사 이름 추출 (issuerCode 기반 변환)
        String cardCompany = "알 수 없는 카드사";
        String cardNumber = "****-****-****-****";

        if (tossResponse.card() != null) {
            var card = tossResponse.card();

            // 1순위: issuerCode가 있으면 공인 코드 테이블에서 매핑
            if (card.issuerCode() != null && !card.issuerCode().isBlank()) {
                cardCompany = TossCardCode.getCardNameByCode(card.issuerCode());
            }
            // 2순위: 혹시라도 company 필드가 채워져서 올 때를 위한 하위 호환성 방어 코드
            else if (card.company() != null && !card.company().isBlank()) {
                cardCompany = card.company();
            }

            if (card.number() != null && !card.number().isBlank()) {
                cardNumber = card.number();
            }
        }
        // 2. 빌링 정보 엔티티 생성 및 저장
        BillingInfo billingInfo = BillingInfo.create(
                member,
                tossResponse.customerKey(),
                tossResponse.billingKey(),
                cardCompany,
                cardNumber
        );

        BillingInfo savedBillingInfo = billingInfoRepository.save(billingInfo);

        return BillingIssueResponse.from(savedBillingInfo);
    }

    /**
     * 회원의 빌링 키 리스트 조회
     */
    public List<BillingDetailsResponse> getBillingKeys(Long memberId) {
        return billingInfoRepository.findByMemberIdAndStatusOrderByCreatedAtDesc(memberId, BillingStatus.ACTIVE)
                .stream()
                .map(BillingDetailsResponse::from)
                .toList();
    }

    // 검증을 위한 조회 트랜잭션 (성능을 위해 readOnly 설정)
    @Transactional(readOnly = true)
    public BillingInfo validateBeforeDelete(Long billingInfoId, Long memberId) {
        return billingInfoRepository.findByIdAndMemberIdAndStatus(billingInfoId, memberId, BillingStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN_REQUEST));
    }

    // 최종 상태 변경을 위한 쓰기 트랜잭션
    @Transactional
    public void deleteBillingInfoStatus(Long billingInfoId) {
        // 영속성 컨텍스트를 새로 얻어와 더티 체킹으로 상태 변경
        BillingInfo billingInfo = billingInfoRepository.findById(billingInfoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_BILLING_INFO));

        billingInfo.delete(); // status = DELETED 변경
    }
}