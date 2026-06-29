package come.back.gotoday.place.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.place.dto.PlaceCreateRequest;
import come.back.gotoday.place.dto.PlaceResponse;
import come.back.gotoday.place.dto.PlaceSearchResponse;
import come.back.gotoday.place.dto.ReverseGeocodingResponse;
import come.back.gotoday.place.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
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
        log.info("장소 생성 요청: name={}", request.name());
        Long placeId = placeService.createPlace(request);
        log.info("장소 생성 응답: placeId={}", placeId);
        return ResponseEntity.ok(
                ApiResponse.success(placeId, "장소 생성 성공")
        );
    }

    // 장소 단건 조회
    @GetMapping("/{placeId}")
    public ResponseEntity<ApiResponse<PlaceResponse>> getPlace(
            @PathVariable Long placeId
    ) {
        log.info("장소 단건 조회 요청: placeId={}", placeId);
        PlaceResponse response = placeService.getPlace(placeId);
        log.info("장소 단건 조회 응답: placeId={}", placeId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "장소 조회 성공")
        );
    }

    // 장소 전체 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> getPlaces() {
        log.info("장소 목록 조회 요청");

        List<PlaceResponse> response = placeService.getPlaces();

        log.info("장소 목록 조회 응답: resultCount={}", response.size());
        return ResponseEntity.ok(
                ApiResponse.success(response, "장소 목록 조회 성공")
        );
    }

    // 장소 검색
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PlaceSearchResponse>>> searchPlaces(
            @RequestParam String query
    ) {
        log.info("장소 검색 요청: query={}", query);

        List<PlaceSearchResponse> response = placeService.searchPlaces(query);

        log.info("장소 검색 응답: query={}, resultCount={}", query, response.size());
        return ResponseEntity.ok(
                ApiResponse.success(response, "장소 검색 성공")
        );
    }

    @GetMapping("/reverse-geocode")
    public ResponseEntity<ApiResponse<ReverseGeocodingResponse>> reverseGeocode(
            @RequestParam Double latitude,
            @RequestParam Double longitude
    ) {
        log.info("좌표 기반 지역명 조회 요청: latitude={}, longitude={}", latitude, longitude);

        ReverseGeocodingResponse response = placeService.reverseGeocode(latitude, longitude);

        log.info(
                "좌표 기반 지역명 조회 응답: latitude={}, longitude={}, areaName={}",
                latitude,
                longitude,
                response.areaName()
        );

        return ResponseEntity.ok(
                ApiResponse.success(response, "좌표 기반 지역명 조회 성공")
        );
    }

    // 장소 삭제
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{placeId}")
    public ResponseEntity<ApiResponse<Void>> deletePlace(
            @PathVariable Long placeId
    ) {
        log.info("장소 삭제 요청: placeId={}", placeId);
        placeService.deletePlace(placeId);
        log.info("장소 삭제 응답: placeId={}", placeId);
        return ResponseEntity.ok(
                ApiResponse.success(null, "장소 삭제 성공")
        );
    }
}