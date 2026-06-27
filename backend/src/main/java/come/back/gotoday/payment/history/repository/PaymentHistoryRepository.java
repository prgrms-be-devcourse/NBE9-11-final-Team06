package come.back.gotoday.payment.history.repository;

import come.back.gotoday.payment.history.entity.PaymentHistory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findBySubscriptionId(Long id);

    Optional<PaymentHistory> findByOrderId(String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.orderId = :orderId")
    Optional<PaymentHistory> findByOrderIdForUpdate(@Param("orderId") String orderId);

    /**
     * 특정 회원의 전체 결제 내역 조회 (구독 -> 빌링정보 -> 회원 연관관계를 타고 들어가 조회)
     */
    @Query("SELECT ph FROM PaymentHistory ph " +
            "JOIN FETCH ph.subscription s " +
            "JOIN s.billingInfo b " +
            "WHERE b.member.id = :memberId " +
            "ORDER BY ph.createdAt DESC")
    List<PaymentHistory> findAllByMemberId(@Param("memberId") Long memberId);

    /**
     * 취소 대상 결제 건 단건 조회 및 소유권 검증용
     */
    @Query("SELECT ph FROM PaymentHistory ph " +
            "JOIN FETCH ph.subscription s " +
            "JOIN s.billingInfo b " +
            "WHERE ph.id = :paymentHistoryId AND b.member.id = :memberId")
    Optional<PaymentHistory> findByIdAndMemberId(
            @Param("paymentHistoryId") Long paymentHistoryId,
            @Param("memberId") Long memberId
    );
}