package come.back.gotoday.payment.billing.repository;

import come.back.gotoday.payment.billing.entity.BillingInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingInfoRepository extends JpaRepository<BillingInfo, Long> {
    // 회원의 모든 등록된 카드(빌링 정보) 조회 (최신등록순)
    List<BillingInfo> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    // 특정 회원의 특정 빌링 정보 단건 조회 (보안 및 소유권 검증용)
    Optional<BillingInfo> findByIdAndMemberId(Long id, Long memberId);
}
