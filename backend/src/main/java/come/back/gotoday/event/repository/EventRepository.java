package come.back.gotoday.event.repository;

import come.back.gotoday.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 외부 API 고유 ID(external_id)를 통해 기존에 저장된 이벤트가 있는지 조회합니다.
     * event 테이블의 `external_id` 컬럼을 매핑 조건으로 사용합니다.
     */
    Optional<Event> findByExternalId(String externalId);

    @Modifying
    @Query("""
            DELETE FROM Event e
            WHERE e.endDate < :today
              AND NOT EXISTS (
                  SELECT 1
                  FROM CoursePlace cp
                  WHERE cp.event.id = e.id
              )
            """)
    int deleteExpiredEvents(@Param("today") LocalDate today);

    List<Event> findByExternalIdIn(Collection<String> externalIds);

    @Query("SELECT e FROM Event e JOIN FETCH e.category") // Event 엔티티 내에 Category 연관관계가 설정되어 있어야 합니다.
    List<Event> findAllWithCategory();

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.place " +
            "WHERE e.id IN :ids")
    List<Event> findAllByIdsWithCategoryAndPlace(@Param("ids") List<Long> ids);


    // 지역, 기간, 매핑된 EVENT 카테고리 ID가 일치하는 행사 조회 (1단계)
    @Query("SELECT e FROM Event e JOIN FETCH e.category c WHERE e.area = :area " +
            "AND e.endDate >= :startDate " +
            "AND e.startDate <= :endDate " +
            "AND c.id IN :categoryIds")
    List<Event> findRecommendedEventsWithCategoryIds(
            @Param("area") String area,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryIds") Collection<Long> categoryIds
    );

    // [기존 유지] 카테고리 상관없이 지역/기간만 맞으면 가져옴 (2단계/Fallback용)
    @Query("SELECT e FROM Event e JOIN FETCH e.category WHERE e.area = :area " +
            "AND e.endDate >= :startDate " +
            "AND e.startDate <= :endDate")
    List<Event> findRecommendedEvents(
            @Param("area") String area,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // [기존 유지] 전체 검색
    @Query("SELECT e FROM Event e JOIN FETCH e.category WHERE e.endDate >= :start AND e.startDate <= :end")
    List<Event> findAllEventsByDate(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select e from Event e " +
            "join fetch e.category " +
            "left join fetch e.place " +
            "where e.id = :eventId")
    Optional<Event> findByIdWithFetch(@Param("eventId") Long eventId);

    @Query("select e from Event e " +
            "join fetch e.category c " +
            "where (:area is null or e.area = :area) " +
            "and (:categoryId is null or c.id = :categoryId) " +
            "and (:keyword is null or e.title like %:keyword%) " +
            "and (" +
            "    :status is null " +
            "    or (:status = 'UPCOMING' and e.startDate > :now) " +
            "    or (:status = 'ING' and e.startDate <= :now and e.endDate >= :now) " +
            "    or (:status = 'END' and e.endDate < :now)" +
            ")")
    Page<Event> findEventsByFilters(
            @Param("area") String area,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("now") LocalDate now,
            Pageable pageable
    );

    //
//    //지워도 됨 이 아래로는 가중치 설정하기 위해 임시로 추가한 것=============
//    // 과거 30개의 데이터 (Train Set - 학습 및 가중치 탐색용)
//    @Query(value = "SELECT * FROM event ORDER BY created_at ASC, id ASC LIMIT 30", nativeQuery = true)
//    List<Event> findTrainSet();
//
//    // 최근 11개의 데이터 (Test Set - 검증용)
//    @Query(value = "SELECT * FROM event ORDER BY created_at ASC, id ASC LIMIT 11 OFFSET 30", nativeQuery = true)
//    List<Event> findTestSet();
//    // EventRepository.java
//    @Query("SELECT e FROM Event e WHERE e.area = :area AND e.category.name IN :categories")
//    List<Event> findByAreaAndCategories(String area, List<String> categories);
//
//    @Query("SELECT e FROM Event e WHERE e.area = :area")
//    List<Event> findByArea(String area);
}