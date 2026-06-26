package come.back.gotoday.payment.billing.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.payment.billing.dto.BillingDetailsResponse;
import come.back.gotoday.payment.billing.dto.BillingIssueRequest;
import come.back.gotoday.payment.billing.dto.BillingIssueResponse;
import come.back.gotoday.payment.billing.service.BillingFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingFacade billingFacade;

    @PostMapping("/issue")
    public ResponseEntity<ApiResponse<BillingIssueResponse>> issueBillingKey(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody BillingIssueRequest request) {
        System.out.println(request.authKey()+" / 커스텀 키: "+request.customerKey());
        BillingIssueResponse response = billingFacade.issueBillingKey(
                userDetails.getMemberId(),
                idempotencyKey,
                request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "빌링키 발급에 성공했습니다.")
        );
    }

    /**
     * 등록된 결제 카드(빌링키) 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BillingDetailsResponse>>> getBillingKeys(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<BillingDetailsResponse> response = billingFacade.getBillingKeys(userDetails.getMemberId());
        return ResponseEntity.ok(
                ApiResponse.success(response, "등록된 카드 목록 조회가 완료되었습니다.")
        );
    }

    /**
     * 등록된 결제 카드(빌링키) 삭제 및 해지
     */
    @DeleteMapping("/{billingInfoId}")
    public ResponseEntity<ApiResponse<Void>> deleteBillingKey(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("billingInfoId") Long billingInfoId) {
        billingFacade.deleteBillingKey(userDetails.getMemberId(), billingInfoId);
        return ResponseEntity.ok(
                ApiResponse.success(null, "카드가 안전하게 삭제 및 해지되었습니다.")
        );
    }
}