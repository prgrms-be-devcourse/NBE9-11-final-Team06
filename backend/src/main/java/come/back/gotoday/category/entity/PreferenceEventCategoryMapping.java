package come.back.gotoday.category.entity;

import come.back.gotoday.category.type.CategoryType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "preference_event_category_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_preference_event_category_mapping",
                columnNames = {"preference_category_id", "event_category_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreferenceEventCategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preference_category_id", nullable = false)
    private Category preferenceCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_category_id", nullable = false)
    private Category eventCategory;

    private PreferenceEventCategoryMapping(
            Category preferenceCategory,
            Category eventCategory
    ) {
        validateCategoryTypes(preferenceCategory, eventCategory);
        this.preferenceCategory = preferenceCategory;
        this.eventCategory = eventCategory;
    }

    public static PreferenceEventCategoryMapping create(
            Category preferenceCategory,
            Category eventCategory
    ) {
        return new PreferenceEventCategoryMapping(
                preferenceCategory,
                eventCategory
        );
    }

    private static void validateCategoryTypes(
            Category preferenceCategory,
            Category eventCategory
    ) {
        if (preferenceCategory == null || eventCategory == null) {
            throw new IllegalArgumentException("매핑할 카테고리는 null일 수 없습니다.");
        }

        if (preferenceCategory.getType() != CategoryType.PREFERENCE) {
            throw new IllegalArgumentException(
                    "선호 카테고리는 PREFERENCE 타입이어야 합니다."
            );
        }

        if (eventCategory.getType() != CategoryType.EVENT) {
            throw new IllegalArgumentException(
                    "이벤트 카테고리는 EVENT 타입이어야 합니다."
            );
        }
    }
}
