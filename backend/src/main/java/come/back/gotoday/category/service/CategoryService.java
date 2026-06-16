package come.back.gotoday.category.service;

import come.back.gotoday.category.dto.CategoryResponse;
import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.category.type.CategoryType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategories(CategoryType type) {
        List<Category> categories = findCategories(type);

        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    private List<Category> findCategories(CategoryType type) {
        if (type == null) {
            return categoryRepository.findAllByOrderByIdAsc();
        }

        return categoryRepository.findByTypeOrderByIdAsc(type);
    }
}