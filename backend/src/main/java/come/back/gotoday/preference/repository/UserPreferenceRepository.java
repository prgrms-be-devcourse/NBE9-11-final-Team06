package come.back.gotoday.preference.repository;

import come.back.gotoday.preference.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}