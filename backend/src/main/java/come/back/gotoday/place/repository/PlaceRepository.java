package come.back.gotoday.place.repository;

import come.back.gotoday.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    boolean existsByNameAndAddressAndIsActiveTrue(String name, String address);

    boolean existsByNameAndAddressAndIsActiveTrueAndIdNot(
            String name,
            String address,
            Long id
    );

    Optional<Place> findByIdAndIsActiveTrue(Long id);

    Optional<Place> findFirstByNameAndAddressAndIsActiveTrueOrderByIdAsc(
            String name,
            String address
    );

    @Query("""
        SELECT p
        FROM Place p
        WHERE (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%'))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:isActive IS NULL OR p.isActive = :isActive)
          AND (:source IS NULL OR p.source = :source)
        """)
    Page<Place> searchAdminPlaces(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("isActive") Boolean isActive,
            @Param("source") String source,
            Pageable pageable
    );
}
