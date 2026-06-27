package come.back.gotoday.crowd.repository;

import come.back.gotoday.crowd.entity.CrowdStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * 좌표가 저장된 각 혼잡도 지역의 최신 데이터만 조회합니다.
     *
     * 동일한 지역명이 여러 번 저장되어 있어도 createdAt이 가장 최근인 데이터만 반환하며,
     * 사용자 선택 장소와 가장 가까운 혼잡도 지역을 계산할 때 사용합니다.
     *
     * @return 지역별 최신 혼잡도 데이터 목록
     */
    @Query("""
            select cs
            from CrowdStatus cs
            where cs.latitude is not null
              and cs.longitude is not null
              and cs.createdAt = (
                  select max(latest.createdAt)
                  from CrowdStatus latest
                  where latest.areaName = cs.areaName
              )
            """)
    List<CrowdStatus> findLatestByArea();

    /**
     * 각 혼잡도 핫스팟의 최신 저장 데이터만 조회한 뒤,
     * 혼잡도 단계가 높은 순서로 정렬해 반환합니다.
     *
     * VERY_CROWDED → CROWDED → NORMAL → RELAXED 순으로 정렬하며,
     * 같은 혼잡도 단계에서는 예상 인구 상한과 최근 저장 시각이 높은 데이터를 우선합니다.
     *
     * 홈 화면에서 현재 가장 붐비는 서울 지역 TOP N을 조회할 때 사용합니다.
     *
     * @return 혼잡도 단계 내림차순으로 정렬된 지역별 최신 혼잡도 데이터 목록
     */
    @Query("""
            select cs
            from CrowdStatus cs
            where cs.createdAt = (
                select max(latest.createdAt)
                from CrowdStatus latest
                where latest.areaName = cs.areaName
            )
            order by
                case cs.congestionLevel
                    when 'VERY_CROWDED' then 4
                    when 'CROWDED' then 3
                    when 'NORMAL' then 2
                    when 'RELAXED' then 1
                    else 0
                end desc,
                cs.populationMax desc,
                cs.createdAt desc
            """)
    List<CrowdStatus> findLatestByAreaOrderByCongestionDesc(Pageable pageable);

    /**
     * 특정 지역의 지정된 기간 내 혼잡도 이력을 측정 시각 오름차순으로 조회합니다.
     *
     * 미래 일정의 혼잡도를 예측할 때 최근 일정 기간의 데이터를 조회한 뒤,
     * 서비스 계층에서 방문 예정일과 동일한 요일 및 시간대만 필터링하는 데 사용합니다.
     *
     * @param areaName 조회할 서울시 핫스팟 장소명
     * @param startAt 조회 시작 시각(포함)
     * @param endAt 조회 종료 시각(포함)
     * @return 기간 내 저장된 혼잡도 이력 목록
     */
    List<CrowdStatus> findAllByAreaNameAndMeasuredAtBetweenOrderByMeasuredAtAsc(
            String areaName,
            LocalDateTime startAt,
            LocalDateTime endAt
    );
}