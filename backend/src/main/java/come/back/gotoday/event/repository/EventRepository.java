package come.back.gotoday.event.repository;
import come.back.gotoday.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 외부 API 고유 ID(external_id)를 통해 기존에 저장된 이벤트가 있는지 조회합니다.
     * event 테이블의 `external_id` 컬럼을 매핑 조건으로 사용합니다.
     */
    Optional<Event> findByExternalId(String externalId);

    @Modifying
    @Query("DELETE FROM Event e WHERE e.endDate < :today")
    int deleteExpiredEvents(@Param("today") LocalDate today);
}