package come.back.gotoday.preference.dto;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.type.CategoryType;

public record PreferenceCategoryResponse(
        Long id,
        String name,
        CategoryType type
) {

    public static PreferenceCategoryResponse from(Category category) {
        return new PreferenceCategoryResponse(
                category.getId(),
                category.getName(),
                category.getType()
        );
    }
}