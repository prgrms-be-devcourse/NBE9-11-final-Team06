package come.back.gotoday.payment.plan.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.payment.plan.dto.PlanResponse;
import come.back.gotoday.payment.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getPlans() {
        List<PlanResponse> plans = planService.getActivePlans();
        return ResponseEntity.ok(
                ApiResponse.success(plans, "플랜 목록 조회가 완료되었습니다.")
        );
    }
}