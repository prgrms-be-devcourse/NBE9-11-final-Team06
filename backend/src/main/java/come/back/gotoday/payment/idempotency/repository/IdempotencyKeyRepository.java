package come.back.gotoday.payment.idempotency.repository;

import come.back.gotoday.member.entity.Member;
import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByMemberAndIdempotencyKey(Member member, String idempotencyKey);

    @Modifying(clearAutomatically = true) // 삭제 후 1차 캐시를 비워 동기화 유시
    @Query("DELETE FROM IdempotencyKey i WHERE i.createdAt < :thresholdDateTime")
    int deleteExpiredKeys(@Param("thresholdDateTime") LocalDateTime thresholdDateTime);
}
