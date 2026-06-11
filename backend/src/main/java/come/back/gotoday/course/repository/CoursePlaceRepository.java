package come.back.gotoday.course.repository;

import come.back.gotoday.course.entity.CoursePlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {
    List<CoursePlace> findByCourseIdOrderByVisitOrder(Long courseId);

    void deleteByCourseId(Long courseId);
}
