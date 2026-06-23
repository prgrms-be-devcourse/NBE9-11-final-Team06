package come.back.gotoday.place.repository;

import come.back.gotoday.place.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    Optional<Place> findBySourceAndExternalId(String source, String externalId);

    boolean existsBySourceAndExternalId(String source, String externalId);

    @Query("""
        SELECT p
        FROM Place p
        WHERE p.isActive = true
          AND p.source = :source
          AND (:area IS NULL OR p.address LIKE CONCAT('%', :area, '%'))
        ORDER BY p.id ASC
        """)
    List<Place> findActivePlacesBySourceAndArea(
            @Param("source") String source,
            @Param("area") String area
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