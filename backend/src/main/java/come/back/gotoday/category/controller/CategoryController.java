package come.back.gotoday.category.controller;

import come.back.gotoday.category.dto.CategoryResponse;
import come.back.gotoday.category.service.CategoryService;
import come.back.gotoday.category.type.CategoryType;
import come.back.gotoday.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @RequestParam(required = false) CategoryType type
    ) {
        List<CategoryResponse> response = categoryService.getCategories(type);

        return ResponseEntity.ok(
                ApiResponse.success(response, "카테고리 목록 조회에 성공했습니다.")
        );
    }
}