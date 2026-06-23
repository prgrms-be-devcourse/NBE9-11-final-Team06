package come.back.gotoday.payment.idempotency.repository;

import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
}
