package come.back.gotoday.place.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.place.dto.PlaceCreateRequest;
import come.back.gotoday.place.dto.PlaceResponse;
import come.back.gotoday.place.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    // 장소 생성
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createPlace(
            @Valid @RequestBody PlaceCreateRequest request
    ) {
        Long placeId = placeService.createPlace(request);

        return ResponseEntity.ok(
                ApiResponse.success(placeId, "장소 생성 성공")
        );
    }

    // 장소 단건 조회
    @GetMapping("/{placeId}")
    public ResponseEntity<ApiResponse<PlaceResponse>> getPlace(
            @PathVariable Long placeId
    ) {
        PlaceResponse response = placeService.getPlace(placeId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "장소 조회 성공")
        );
    }

    // 장소 전체 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> getPlaces() {

        List<PlaceResponse> response = placeService.getPlaces();

        return ResponseEntity.ok(
                ApiResponse.success(response, "장소 목록 조회 성공")
        );
    }

    // 장소 삭제
    @DeleteMapping("/{placeId}")
    public ResponseEntity<ApiResponse<Void>> deletePlace(
            @PathVariable Long placeId
    ) {
        placeService.deletePlace(placeId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "장소 삭제 성공")
        );
    }
}