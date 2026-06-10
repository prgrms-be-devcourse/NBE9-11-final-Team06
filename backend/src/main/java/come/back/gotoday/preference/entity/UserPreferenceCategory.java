package come.back.gotoday.preference.entity;

import come.back.gotoday.category.entity.Category;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_preference_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreferenceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_preference_id", nullable = false)
    private UserPreference userPreference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private UserPreferenceCategory(UserPreference userPreference, Category category) {
        this.userPreference = userPreference;
        this.category = category;
    }

    // [규칙 반영] 정적 팩토리 메서드
    public static UserPreferenceCategory create(UserPreference userPreference, Category category) {
        return new UserPreferenceCategory(userPreference, category);
    }
}
