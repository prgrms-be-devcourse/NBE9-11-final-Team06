package come.back.gotoday.preference.repository;

import come.back.gotoday.preference.entity.UserPreferenceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserPreferenceCategoryRepository extends JpaRepository<UserPreferenceCategory, Long> {

    @Query("""
            select upc
            from UserPreferenceCategory upc
            join fetch upc.category
            where upc.userPreference.id = :userPreferenceId
            """)
    List<UserPreferenceCategory> findByUserPreferenceIdWithCategory(
            @Param("userPreferenceId") Long userPreferenceId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from UserPreferenceCategory upc
            where upc.userPreference.id = :userPreferenceId
            """)
    void deleteByUserPreferenceId(@Param("userPreferenceId") Long userPreferenceId);
}