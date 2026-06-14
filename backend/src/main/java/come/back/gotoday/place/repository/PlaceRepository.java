package come.back.gotoday.place.repository;

import come.back.gotoday.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    boolean existsByNameAndAddressAndIsActiveTrue(String name, String address);

    Optional<Place> findByIdAndIsActiveTrue(Long id);
}
