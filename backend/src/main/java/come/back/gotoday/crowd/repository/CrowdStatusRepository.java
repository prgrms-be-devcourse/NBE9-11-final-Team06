package come.back.gotoday.crowd.repository;

import come.back.gotoday.crowd.entity.CrowdStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 혼잡도 데이터(CrowdStatus)를 조회하고 저장하기 위한 Repository입니다.
 *
 * JpaRepository를 상속해 기본적인 CRUD 기능을 사용할 수 있으며,
 * 지역명 기준 최신 혼잡도 데이터를 조회하는 메서드를 추가로 제공합니다.
 */
public interface CrowdStatusRepository extends JpaRepository<CrowdStatus, Long> {

    /**
     * 특정 핫스팟 장소명의 가장 최근 저장 데이터를 조회합니다.
     *
     * 캐시 유효 시간은 서울시 API의 측정 시각(measuredAt)이 아니라
     * 우리 서버가 데이터를 저장한 시각(createdAt)을 기준으로 판단합니다.
     *
     * @param areaName 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소명
     * @return 해당 장소의 가장 최근 저장 데이터가 있으면 Optional에 담아 반환
     */
    Optional<CrowdStatus> findTopByAreaNameOrderByCreatedAtDesc(String areaName);
}