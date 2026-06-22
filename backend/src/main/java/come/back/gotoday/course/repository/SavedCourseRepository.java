package come.back.gotoday.course.repository;

import come.back.gotoday.course.entity.SavedCourse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {

    Optional<SavedCourse> findByMemberIdAndCourseId(Long memberId, Long courseId);

    boolean existsByMemberIdAndCourseId(Long memberId, Long courseId);

    @EntityGraph(attributePaths = {"course"})
    List<SavedCourse> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);
}