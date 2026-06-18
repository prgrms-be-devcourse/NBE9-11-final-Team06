package come.back.gotoday.preference.repository;

import come.back.gotoday.preference.entity.UserPreferenceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
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

    @Query("""
            select c.name
            from UserPreferenceCategory upc
            join upc.category c
            where upc.userPreference.id = :preferenceId
            """)
    List<String> findCategoryNamesByPreferenceId(@Param("preferenceId") Long preferenceId);

    @Query("""
            select c.id
            from UserPreferenceCategory upc
            join upc.category c
            where upc.userPreference.id = :preferenceId
            """)
    List<Long> findCategoryIdsByPreferenceId(@Param("preferenceId") Long preferenceId);
}