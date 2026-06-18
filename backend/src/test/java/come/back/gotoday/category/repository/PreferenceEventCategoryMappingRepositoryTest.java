package come.back.gotoday.category.repository;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.entity.PreferenceEventCategoryMapping;
import come.back.gotoday.category.type.CategoryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PreferenceEventCategoryMappingRepositoryTest {

    @Autowired
    private PreferenceEventCategoryMappingRepository mappingRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("선호 카테고리 ID로 매핑된 이벤트 카테고리 ID를 조회한다")
    void findEventCategoryIdsByPreferenceCategoryIds() {
        Category exhibitionPreference = saveCategory(
                "테스트-전시",
                CategoryType.PREFERENCE
        );
        Category exhibitionEvent = saveCategory(
                "테스트-전시/미술",
                CategoryType.EVENT
        );
        saveMapping(exhibitionPreference, exhibitionEvent);

        List<Long> result = mappingRepository
                .findEventCategoryIdsByPreferenceCategoryIds(
                        List.of(exhibitionPreference.getId())
                );

        assertThat(result).containsExactly(exhibitionEvent.getId());
    }

    @Test
    @DisplayName("선호 카테고리 이름으로 여러 이벤트 카테고리 ID를 조회한다")
    void findEventCategoryIdsByPreferenceCategoryNames() {
        Category performancePreference = saveCategory(
                "테스트-공연",
                CategoryType.PREFERENCE
        );
        Category concertEvent = saveCategory(
                "테스트-콘서트",
                CategoryType.EVENT
        );
        Category musicalEvent = saveCategory(
                "테스트-뮤지컬/오페라",
                CategoryType.EVENT
        );

        saveMapping(performancePreference, concertEvent);
        saveMapping(performancePreference, musicalEvent);

        List<Long> result = mappingRepository
                .findEventCategoryIdsByPreferenceCategoryNames(
                        List.of(performancePreference.getName())
                );

        assertThat(result).containsExactlyInAnyOrder(
                concertEvent.getId(),
                musicalEvent.getId()
        );
    }

    @Test
    @DisplayName("여러 선호 카테고리가 같은 이벤트 카테고리에 연결되어도 중복 없이 조회한다")
    void findEventCategoryIdsWithoutDuplicates() {
        Category culturePreference = saveCategory(
                "테스트-문화",
                CategoryType.PREFERENCE
        );
        Category artPreference = saveCategory(
                "테스트-예술",
                CategoryType.PREFERENCE
        );
        Category exhibitionEvent = saveCategory(
                "테스트-공통전시",
                CategoryType.EVENT
        );

        saveMapping(culturePreference, exhibitionEvent);
        saveMapping(artPreference, exhibitionEvent);

        List<Long> result = mappingRepository
                .findEventCategoryIdsByPreferenceCategoryIds(
                        List.of(
                                culturePreference.getId(),
                                artPreference.getId()
                        )
                );

        assertThat(result).containsExactly(exhibitionEvent.getId());
    }

    @Test
    @DisplayName("매핑되지 않은 선호 카테고리를 조회하면 빈 목록을 반환한다")
    void findEventCategoryIdsWhenMappingDoesNotExist() {
        Category unmappedPreference = saveCategory(
                "테스트-미매핑",
                CategoryType.PREFERENCE
        );

        List<Long> result = mappingRepository
                .findEventCategoryIdsByPreferenceCategoryIds(
                        List.of(unmappedPreference.getId())
                );

        assertThat(result).isEmpty();
    }

    private Category saveCategory(String name, CategoryType type) {
        return entityManager.persistAndFlush(Category.create(name, type));
    }

    private void saveMapping(
            Category preferenceCategory,
            Category eventCategory
    ) {
        mappingRepository.saveAndFlush(
                PreferenceEventCategoryMapping.create(
                        preferenceCategory,
                        eventCategory
                )
        );
    }
}
