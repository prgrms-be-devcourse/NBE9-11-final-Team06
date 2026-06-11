package come.back.gotoday.course.repository;

import come.back.gotoday.course.entity.SavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {
}