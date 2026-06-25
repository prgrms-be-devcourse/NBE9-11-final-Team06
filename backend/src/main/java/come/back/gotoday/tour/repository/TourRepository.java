package come.back.gotoday.tour.repository;

import come.back.gotoday.tour.entity.Tour;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TourRepository extends JpaRepository<Tour, Long> {

    Optional<Tour> findByContentId(String contentId);

    boolean existsByContentId(String contentId);

    @Query("""
            SELECT t
            FROM Tour t
            WHERE t.isActive = true
              AND t.latitude IS NOT NULL
              AND t.longitude IS NOT NULL
              AND (
                    :area IS NULL
                    OR :area = ''
                    OR t.area LIKE CONCAT('%', :area, '%')
                    OR t.address LIKE CONCAT('%', :area, '%')
              )
            ORDER BY t.id DESC
            """)
    List<Tour> findRecommendedToursByArea(
            @Param("area") String area,
            Pageable pageable
    );
}