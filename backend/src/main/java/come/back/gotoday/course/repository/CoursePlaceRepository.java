package come.back.gotoday.course.repository;

import come.back.gotoday.course.entity.CoursePlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {
    List<CoursePlace> findByCourseIdOrderByVisitOrder(Long courseId);

    void deleteByCourseId(Long courseId);

    // 💡 join fetch를 left join fetch로 수정해서 place가 null이어도 다 긁어오게 변경!
    @Query("""
        select cp from CoursePlace cp
        left join fetch cp.place
        left join fetch cp.event e
        left join fetch e.place
        where cp.course.id = :courseId
        order by cp.visitOrder
    """)
    List<CoursePlace> findDetailByCourseId(@Param("courseId") Long courseId);
}