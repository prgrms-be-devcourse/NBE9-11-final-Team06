package come.back.gotoday.category.repository;

import come.back.gotoday.category.entity.PreferenceEventCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PreferenceEventCategoryMappingRepository
        extends JpaRepository<PreferenceEventCategoryMapping, Long> {

    @Query("""
            select distinct mapping.eventCategory.id
            from PreferenceEventCategoryMapping mapping
            where mapping.preferenceCategory.id in :preferenceCategoryIds
            """)
    List<Long> findEventCategoryIdsByPreferenceCategoryIds(
            @Param("preferenceCategoryIds")
            Collection<Long> preferenceCategoryIds
    );

    @Query("""
        select distinct mapping.eventCategory.id
        from PreferenceEventCategoryMapping mapping
        where mapping.preferenceCategory.name in :preferenceCategoryNames
        """)
    List<Long> findEventCategoryIdsByPreferenceCategoryNames(
            @Param("preferenceCategoryNames")
            Collection<String> preferenceCategoryNames
    );

    boolean existsByPreferenceCategoryIdAndEventCategoryId(
            Long preferenceCategoryId,
            Long eventCategoryId
    );
}
