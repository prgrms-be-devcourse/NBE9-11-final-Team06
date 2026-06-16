package come.back.gotoday.admin.controller;

import come.back.gotoday.admin.dto.request.AdminPlaceCreateRequest;
import come.back.gotoday.admin.dto.request.AdminPlaceUpdateRequest;
import come.back.gotoday.admin.dto.response.AdminPlaceResponse;
import come.back.gotoday.admin.service.AdminPlaceService;
import come.back.gotoday.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@RestController
@RequestMapping("/api/admin/places")
@RequiredArgsConstructor
public class AdminPlaceController {

    private final AdminPlaceService adminPlaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminPlaceResponse>> createPlace(
            @Valid @RequestBody AdminPlaceCreateRequest request
    ) {
        log.info("관리자 장소 등록 요청: name={}", request.name());

        AdminPlaceResponse response = adminPlaceService.createPlace(request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "관리자 장소 등록 성공")
        );
    }

    @PutMapping("/{placeId}")
    public ResponseEntity<ApiResponse<AdminPlaceResponse>> updatePlace(
            @PathVariable Long placeId,
            @Valid @RequestBody AdminPlaceUpdateRequest request
    ) {
        log.info("관리자 장소 수정 요청: placeId={}", placeId);

        AdminPlaceResponse response = adminPlaceService.updatePlace(placeId, request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "관리자 장소 수정 성공")
        );
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<ApiResponse<Void>> deletePlace(
            @PathVariable Long placeId
    ) {
        log.info("관리자 장소 비활성화 요청: placeId={}", placeId);

        adminPlaceService.deletePlace(placeId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "관리자 장소 비활성화 성공")
        );
    }

    @GetMapping
    public ResponseEntity<Page<AdminPlaceResponse>> getPlaces(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String source,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                adminPlaceService.getPlaces(keyword, categoryId, isActive, source, pageable)
        );
    }
}