package come.back.gotoday.payment.plan.repository;

import come.back.gotoday.payment.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan,Long> {
    List<Plan> findAllByIsActiveTrue();
}
