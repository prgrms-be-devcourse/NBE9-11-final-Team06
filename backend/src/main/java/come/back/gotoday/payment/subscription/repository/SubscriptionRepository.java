package come.back.gotoday.payment.subscription.repository;

import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // 특정 회원의 활성화된 구독이 있는지 확인 (중복 구독 방지용)
    @Query("SELECT s FROM Subscription s JOIN s.billingInfo b WHERE b.member.id = :memberId AND s.status = :status")
    Optional<Subscription> findActiveSubscriptionByMemberId(@Param("memberId") Long memberId, @Param("status") SubscriptionStatus status);

    // 스케줄러 배치용: 오늘이 결제일이면서 활성화 상태인 구독 목록 조회
    List<Subscription> findAllByNextBillingDateAndStatus(LocalDate nextBillingDate, SubscriptionStatus status);

    @Query("SELECT COUNT(s) > 0 FROM Subscription s JOIN s.billingInfo b WHERE b.member.id = :memberId AND s.status IN :statuses")
    boolean existsByMemberIdAndStatusIn(@Param("memberId") Long memberId, @Param("statuses") Collection<SubscriptionStatus> statuses);
}