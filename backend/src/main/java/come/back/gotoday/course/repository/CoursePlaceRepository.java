package come.back.gotoday.course.repository;

import come.back.gotoday.course.entity.CoursePlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {
    List<CoursePlace> findByCourseIdOrderByVisitOrder(Long courseId);

    void deleteByCourseId(Long courseId);


    @Query("""
        select cp from CoursePlace cp
        join fetch cp.place
        where cp.course.id = :courseId
        order by cp.visitOrder
    """)
    List<CoursePlace> findDetailByCourseId(@Param("courseId") Long courseId);
}
