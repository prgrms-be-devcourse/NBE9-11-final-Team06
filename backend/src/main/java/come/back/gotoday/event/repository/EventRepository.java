package come.back.gotoday.event.repository;
import come.back.gotoday.event.entity.Event;
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
    @Query("DELETE FROM Event e WHERE e.endDate < :today")
    int deleteExpiredEvents(@Param("today") LocalDate today);

    List<Event> findByExternalIdIn(Collection<String> externalIds);

    @Query("SELECT e FROM Event e JOIN FETCH e.category") // Event 엔티티 내에 Category 연관관계가 설정되어 있어야 합니다.
    List<Event> findAllWithCategory();

    @Query(value = "SELECT e.* FROM event e " +
            "INNER JOIN category c ON e.category_id = c.id " +
            "WHERE e.embedding_vector IS NOT NULL",
            nativeQuery = true)
    List<Event> findAllReadyEventsWithCategory();

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.place " +
            "WHERE e.id IN :ids")
    List<Event> findAllByIdsWithCategoryAndPlace(@Param("ids") List<Long> ids);

    // [추가] 지역, 기간, 카테고리까지 완벽 일치하는 행사를 찾을 때 사용 (1단계)
    @Query("SELECT e FROM Event e WHERE e.area = :area " +
            "AND e.endDate >= :startDate " +
            "AND e.startDate <= :endDate " +
            "AND e.category.name IN :categories")
    List<Event> findRecommendedEventsWithCategory(
            @Param("area") String area,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categories") List<String> categories
    );

    // [기존 유지] 카테고리 상관없이 지역/기간만 맞으면 가져옴 (2단계/Fallback용)
    @Query("SELECT e FROM Event e WHERE e.area = :area " +
            "AND e.endDate >= :startDate " +
            "AND e.startDate <= :endDate")
    List<Event> findRecommendedEvents(
            @Param("area") String area,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

//    // [기존 유지] 전체 검색
//    @Query("SELECT e FROM Event e WHERE e.endDate >= :start AND e.startDate <= :end")
//    List<Event> findAllEventsByDate(@Param("start") LocalDate start, @Param("end") LocalDate end);
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