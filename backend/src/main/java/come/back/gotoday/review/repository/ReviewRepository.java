package come.back.gotoday.review.repository;

import come.back.gotoday.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("select r from Review r join fetch r.member where r.course.id = :courseId")
    List<Review> findAllByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.course.id = :courseId")
    int countByCourseId(Long courseId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.course.id = :courseId")
    Double findAverageRating(Long courseId);

    boolean existsByMemberIdAndCourseId(Long memberId, Long courseId);

    Optional<Review> findByCourseIdAndMemberId(Long courseId, Long memberId);

}
