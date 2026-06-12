package come.back.gotoday.global.initData;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.category.type.CategoryType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CategoryInitData {

    private final CategoryRepository categoryRepository;

    @Bean
    public ApplicationRunner initCategory() {
        return args -> {

            if (categoryRepository.count() > 0) {
                return; // 중복 방지
            }

            categoryRepository.save(Category.create("카페", CategoryType.PLACE));
            categoryRepository.save(Category.create("맛집", CategoryType.PLACE));
            categoryRepository.save(Category.create("관광지", CategoryType.PLACE));

            categoryRepository.save(Category.create("축제", CategoryType.EVENT));
            categoryRepository.save(Category.create("전시", CategoryType.EVENT));

            categoryRepository.save(Category.create("조용한", CategoryType.PREFERENCE));
            categoryRepository.save(Category.create("활동적인", CategoryType.PREFERENCE));
        };
    }
}