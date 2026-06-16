package come.back.gotoday.place.repository;

import come.back.gotoday.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    boolean existsByNameAndAddressAndIsActiveTrue(String name, String address);

    boolean existsByNameAndAddressAndIsActiveTrueAndIdNot(
            String name,
            String address,
            Long id
    );

    Optional<Place> findByIdAndIsActiveTrue(Long id);

    Optional<Place> findByName(String name);

}
