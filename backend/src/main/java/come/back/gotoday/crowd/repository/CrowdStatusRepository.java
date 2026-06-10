package come.back.gotoday.crowd.repository;

import come.back.gotoday.crowd.entity.CrowdStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrowdStatusRepository extends JpaRepository<CrowdStatus, Long> {

    Optional<CrowdStatus> findTopByAreaNameOrderByMeasuredAtDesc(String areaName);
}