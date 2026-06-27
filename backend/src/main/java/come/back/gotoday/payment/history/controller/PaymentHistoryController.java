package come.back.gotoday.payment.history.controller;


import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.payment.history.dto.PaymentHistoryResponse;
import come.back.gotoday.payment.subscription.dto.SubscriptionPaymentCancelRequest;
import come.back.gotoday.payment.history.service.PaymentHistoryFacade;
import come.back.gotoday.payment.history.service.PaymentHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions/payments")
@RequiredArgsConstructor
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;
    private final PaymentHistoryFacade paymentHistoryFacade;

    /**
     * 내 결제 내역 리스트 조회 API
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getMyPaymentHistories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<PaymentHistoryResponse> response = paymentHistoryService.getPaymentHistories(userDetails.getMemberId());
        return ResponseEntity.ok(
                ApiResponse.success(response, "결제 내역 조회가 완료되었습니다.")
        );
    }

    /**
     * 특정 결제 건 취소 API
     */
    @PostMapping("/{paymentHistoryId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("paymentHistoryId") Long paymentHistoryId,
            @Valid @RequestBody SubscriptionPaymentCancelRequest request) {

        paymentHistoryFacade.cancelPayment(userDetails.getMemberId(), paymentHistoryId, request);
        return ResponseEntity.ok(
                ApiResponse.success(null, "결제 취소 요청이 정상적으로 처리되었습니다.")
        );
    }
}