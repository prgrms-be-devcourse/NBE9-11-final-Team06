package come.back.gotoday.payment.settlement.controller;
import come.back.gotoday.external.toss.dto.SettlementDto;
import come.back.gotoday.payment.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementControllerTest {

    private final SettlementService settlementService;

    /**
     * 토스페이먼츠 서버 역할을 대신 해주는 가상 채널
     * 테스트용 정산 데이터 주입 API
     * 토스 정산 응답 형식의 가짜 JSON 배열을 Body에 담아 요청을 테스트합니다.
     */
    @PostMapping("/test-reconcile")
    public ResponseEntity<String> testReconcile(@RequestBody List<SettlementDto.TossSettlementResponse> request) {
        settlementService.reconcileSettlement(request);
        return ResponseEntity.ok("정산 대조 테스트 스크립트가 성공적으로 수행되었습니다. DB를 확인하세요.");
    }
}