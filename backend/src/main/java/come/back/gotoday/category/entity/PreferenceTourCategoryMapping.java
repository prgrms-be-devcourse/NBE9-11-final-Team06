package come.back.gotoday.category.entity;

import come.back.gotoday.category.type.CategoryType;
import jakarta.persistence.Column;
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
        name = "preference_tour_category_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_preference_tour_category_mapping",
                columnNames = {"preference_category_id", "tour_cat3"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreferenceTourCategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preference_category_id", nullable = false)
    private Category preferenceCategory;

    @Column(name = "tour_cat3", nullable = false, length = 20)
    private String tourCat3;

    private PreferenceTourCategoryMapping(
            Category preferenceCategory,
            String tourCat3
    ) {
        validate(preferenceCategory, tourCat3);
        this.preferenceCategory = preferenceCategory;
        this.tourCat3 = tourCat3;
    }

    public static PreferenceTourCategoryMapping create(
            Category preferenceCategory,
            String tourCat3
    ) {
        return new PreferenceTourCategoryMapping(
                preferenceCategory,
                tourCat3
        );
    }

    private static void validate(
            Category preferenceCategory,
            String tourCat3
    ) {
        if (preferenceCategory == null) {
            throw new IllegalArgumentException("선호 카테고리는 null일 수 없습니다.");
        }

        if (preferenceCategory.getType() != CategoryType.PREFERENCE) {
            throw new IllegalArgumentException(
                    "선호 카테고리는 PREFERENCE 타입이어야 합니다."
            );
        }

        if (tourCat3 == null || tourCat3.isBlank()) {
            throw new IllegalArgumentException(
                    "관광지 세부 분류 코드(tourCat3)는 비어 있을 수 없습니다."
            );
        }
    }
}