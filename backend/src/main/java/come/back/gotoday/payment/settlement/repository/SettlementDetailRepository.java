package come.back.gotoday.payment.settlement.repository;

import come.back.gotoday.payment.settlement.entity.SettlementDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SettlementDetailRepository extends JpaRepository<SettlementDetail, Long> {
    // 특정 정산일에 정산된 내역 조회 (어드민 대시보드용)
    List<SettlementDetail> findBySettlementDate(LocalDate settlementDate);

    boolean existsByOrderIdAndAmount(String orderId, Long amount);
}