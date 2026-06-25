package come.back.gotoday.payment.subscription.repository;

import come.back.gotoday.payment.subscription.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findBySubscriptionId(Long id);
}