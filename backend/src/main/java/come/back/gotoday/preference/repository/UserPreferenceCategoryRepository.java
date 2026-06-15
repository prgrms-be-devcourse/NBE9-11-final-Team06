package come.back.gotoday.preference.repository;

import come.back.gotoday.preference.entity.UserPreferenceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPreferenceCategoryRepository extends JpaRepository<UserPreferenceCategory, Long> {

    List<UserPreferenceCategory> findByUserPreferenceId(Long userPreferenceId);

    void deleteByUserPreferenceId(Long userPreferenceId);
}