package come.back.gotoday.category.repository;

import come.back.gotoday.category.entity.PreferenceTourCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PreferenceTourCategoryMappingRepository
        extends JpaRepository<PreferenceTourCategoryMapping, Long> {

    @Query("""
            select distinct mapping.tourCat3
            from PreferenceTourCategoryMapping mapping
            where mapping.preferenceCategory.id in :preferenceCategoryIds
            """)
    List<String> findTourCat3CodesByPreferenceCategoryIds(
            @Param("preferenceCategoryIds")
            Collection<Long> preferenceCategoryIds
    );

    @Query("""
            select distinct mapping.tourCat3
            from PreferenceTourCategoryMapping mapping
            where mapping.preferenceCategory.name in :preferenceCategoryNames
            """)
    List<String> findTourCat3CodesByPreferenceCategoryNames(
            @Param("preferenceCategoryNames")
            Collection<String> preferenceCategoryNames
    );

    boolean existsByPreferenceCategoryIdAndTourCat3(
            Long preferenceCategoryId,
            String tourCat3
    );
}