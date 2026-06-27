package come.back.gotoday.global.initData;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.category.type.CategoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CategoryInitData {

    private final CategoryRepository categoryRepository;

    @Bean
    public ApplicationRunner initCategory() {
        return args -> {
            log.info("카테고리 초기 데이터 생성 확인 시작");

            long existingCategoryCount = categoryRepository.count();
            if (existingCategoryCount > 0) {
                log.info("카테고리 초기 데이터가 이미 존재하여 생성을 건너뜁니다. existingCount={}", existingCategoryCount);
                return; // 중복 방지
            }

            categoryRepository.save(Category.create(1L, "카페", CategoryType.PLACE));
            categoryRepository.save(Category.create(2L, "맛집", CategoryType.PLACE));
            categoryRepository.save(Category.create(3L, "관광지", CategoryType.PLACE));

            categoryRepository.save(Category.create(4L, "축제", CategoryType.EVENT));
            categoryRepository.save(Category.create(5L, "전시", CategoryType.EVENT));

            categoryRepository.save(Category.create(8L, "조용한 힐링", CategoryType.PREFERENCE));
            categoryRepository.save(Category.create(9L, "문화생활", CategoryType.PREFERENCE));
            categoryRepository.save(Category.create(10L, "자연•산책", CategoryType.PREFERENCE));
            categoryRepository.save(Category.create(11L, "감성•사진", CategoryType.PREFERENCE));
            categoryRepository.save(Category.create(12L, "활동적인 하루", CategoryType.PREFERENCE));
            categoryRepository.save(Category.create(13L, "새로운 경험", CategoryType.PREFERENCE));

            categoryRepository.save(Category.create(30L, "공연", CategoryType.EVENT));
            categoryRepository.save(Category.create(31L, "교육/체험", CategoryType.EVENT));
            categoryRepository.save(Category.create(32L, "기타행사", CategoryType.EVENT));

            log.info("카테고리 초기 데이터 생성 완료. createdCount={}", categoryRepository.count());
        };
    }
}