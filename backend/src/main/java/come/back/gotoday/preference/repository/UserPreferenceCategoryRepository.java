package come.back.gotoday.preference.repository;

import come.back.gotoday.preference.entity.UserPreferenceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPreferenceCategoryRepository extends JpaRepository<UserPreferenceCategory, Long> {

    List<UserPreferenceCategory> findByUserPreferenceId(Long userPreferenceId);

    void deleteByUserPreferenceId(Long userPreferenceId);
  
    @Query("SELECT c.name FROM UserPreferenceCategory upc " +
            "JOIN upc.category c " +
            "WHERE upc.userPreference.id = :preferenceId")
    List<String> findCategoryNamesByPreferenceId(@Param("preferenceId") Long preferenceId);
}

