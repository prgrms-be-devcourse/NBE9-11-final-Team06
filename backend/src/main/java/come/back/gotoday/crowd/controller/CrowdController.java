package come.back.gotoday.crowd.controller;

import come.back.gotoday.crowd.dto.CrowdResponse;
import come.back.gotoday.crowd.service.CrowdService;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 혼잡도 조회 API를 담당하는 컨트롤러입니다.
 *
 * 클라이언트가 특정 지역명(areaName)을 전달하면,
 * CrowdService를 통해 서울시 실시간 도시데이터 API에서 혼잡도 정보를 조회합니다.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/crowds")
public class CrowdController {

    private final CrowdService crowdService;

    /**
     * CrowdService를 생성자 주입 방식으로 주입받습니다.
     *
     * 생성자 주입은 의존성이 명확하고 테스트하기 쉬워서 Spring에서 권장되는 방식입니다.
     */
    public CrowdController(CrowdService crowdService) {
        this.crowdService = crowdService;
    }

    /**
     * 지역명 기준으로 현재 혼잡도 정보를 조회합니다.
     *
     * 요청 예시:
     * GET /api/crowds?areaName=성수카페거리
     *
     * @param areaName 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소명
     * @return 혼잡도 단계, 메시지, 예상 인구 범위, 측정 시각이 담긴 응답 DTO
     */
    @GetMapping
    public CrowdResponse getCrowdStatus(
            @RequestParam
            @NotBlank(message = "지역명은 필수입니다.")
            String areaName
    ) {
        log.info("혼잡도 조회 요청: areaName={}", areaName);

        CrowdResponse response = crowdService.getCrowdStatus(areaName);

        log.info("혼잡도 조회 응답: areaName={}, congestionLevel={}", areaName, response.congestionLevel());
        return response;
    }
}
