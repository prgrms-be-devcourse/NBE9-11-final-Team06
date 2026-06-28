package come.back.gotoday.review.repository;

import come.back.gotoday.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("select r from Review r join fetch r.member where r.course.id = :courseId")
    List<Review> findAllByCourseId(@Param("courseId") Long courseId);

    boolean existsByMemberIdAndCourseId(Long memberId, Long courseId);
}
