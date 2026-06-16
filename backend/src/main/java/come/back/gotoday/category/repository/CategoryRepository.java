package come.back.gotoday.category.repository;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.type.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByIdIn(Collection<Long> ids);

    List<Category> findAllByOrderByIdAsc();

    List<Category> findByTypeOrderByIdAsc(CategoryType type);
}