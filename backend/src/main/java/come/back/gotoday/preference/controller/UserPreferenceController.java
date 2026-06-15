package come.back.gotoday.preference.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.preference.dto.UserPreferenceCreateRequest;
import come.back.gotoday.preference.dto.UserPreferenceResponse;
import come.back.gotoday.preference.dto.UserPreferenceUpdateRequest;
import come.back.gotoday.preference.service.UserPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> createMyPreference(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserPreferenceCreateRequest request
    ) {
        UserPreferenceResponse response = userPreferenceService.createPreference(
                userDetails.getMemberId(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "선호 정보 등록에 성공했습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> getMyPreference(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserPreferenceResponse response = userPreferenceService.getMyPreference(
                userDetails.getMemberId()
        );

        if (response == null) {
            return ResponseEntity.ok(
                    ApiResponse.success(null, "등록된 선호 정보가 없습니다.")
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(response, "선호 정보 조회에 성공했습니다.")
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> updateMyPreference(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserPreferenceUpdateRequest request
    ) {
        UserPreferenceResponse response = userPreferenceService.updatePreference(
                userDetails.getMemberId(),
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(response, "선호 정보 수정에 성공했습니다.")
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyPreference(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        userPreferenceService.deletePreference(userDetails.getMemberId());

        return ResponseEntity.ok(
                ApiResponse.success("선호 정보 삭제에 성공했습니다.")
        );
    }
}