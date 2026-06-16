package come.back.gotoday.category.dto;

import come.back.gotoday.category.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        String type
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType().name()
        );
    }
}