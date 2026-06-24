package come.back.gotoday.payment.subscription.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.payment.subscription.dto.SubscriptionRequest;
import come.back.gotoday.payment.subscription.dto.SubscriptionResponse;
import come.back.gotoday.payment.subscription.service.SubscriptionFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionFacade subscriptionFacade;

    //정기 구독 신청 (첫 달 즉시 결제 승인 포함)
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> startSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SubscriptionRequest request) {

        SubscriptionResponse response = subscriptionFacade.startSubscription(userDetails.getMemberId(), request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "정기 구독 신청 및 첫 달 결제가 완료되었습니다.")
        );
    }

    //현재 회원의 활성화된 정기 구독 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getMySubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        SubscriptionResponse response = subscriptionFacade.getMyActiveSubscription(userDetails.getMemberId());
        return ResponseEntity.ok(
                ApiResponse.success(response, "현재 이용 중인 구독 정보 조회가 완료되었습니다.")
        );
    }

    //정기 구독 해지 (다음 결제일부터 자동 결제 중단)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long subscriptionId) {

        subscriptionFacade.cancelSubscription(userDetails.getMemberId(), subscriptionId);
        return ResponseEntity.ok(
                ApiResponse.success(null, "정기 구독 해지 신청이 완료되었습니다. 다음 결제일부터 청구되지 않습니다.")
        );
    }
}