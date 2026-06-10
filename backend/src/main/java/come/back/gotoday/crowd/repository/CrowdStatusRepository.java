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
     * 특정 핫스팟 장소명의 가장 최근 혼잡도 데이터를 조회합니다.
     *
     * 메서드 이름 규칙에 따라 Spring Data JPA가 자동으로 쿼리를 생성합니다.
     * measuredAt 기준 내림차순으로 정렬한 뒤 가장 첫 번째 데이터만 가져옵니다.
     *
     * @param areaName 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소명
     * @return 해당 장소의 최신 혼잡도 데이터가 있으면 Optional에 담아 반환
     */
    Optional<CrowdStatus> findTopByAreaNameOrderByMeasuredAtDesc(String areaName);
}