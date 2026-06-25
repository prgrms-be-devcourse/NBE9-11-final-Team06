package come.back.gotoday.tour.repository;

import come.back.gotoday.tour.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TourRepository extends JpaRepository<Tour, Long> {

    Optional<Tour> findByContentId(String contentId);

    boolean existsByContentId(String contentId);

    List<Tour> findTop3ByAreaContainingAndIsActiveTrueOrderByIdDesc(String area);

    List<Tour> findTop3ByAddressContainingAndIsActiveTrueOrderByIdDesc(String address);

    List<Tour> findByAreaContainingAndIsActiveTrue(String area);

    List<Tour> findByAddressContainingAndIsActiveTrue(String address);
}