package come.back.gotoday.payment.subscription.repository;

import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    //배치 처리용: 결제일이 오늘 이하(<=)이면서, ACTIVE 또는 유예 상태(EXPIRED_PAYMENT_PENDING)인 대상을 페이징 조회
    @Query("SELECT s FROM Subscription s " +
            "WHERE s.id > :lastId " +
            "AND s.nextBillingDate <= :today " +
            "AND s.status IN (:statuses)")
    Slice<Subscription> findBillingTargets(
            @Param("lastId") Long lastId,
            @Param("today") LocalDate today,
            @Param("statuses") List<SubscriptionStatus> statuses,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);
}