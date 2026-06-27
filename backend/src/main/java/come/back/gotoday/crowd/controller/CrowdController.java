package come.back.gotoday.crowd.controller;

import come.back.gotoday.crowd.dto.CrowdResponse;
import come.back.gotoday.crowd.service.CrowdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 혼잡도 조회 API를 담당하는 컨트롤러입니다.
 *
 * 좌표가 전달되면 저장된 혼잡도 핫스팟 중 최근접 지역을 반환하고,
 * 좌표가 없을 때만 서울시 공식 핫스팟 지역명으로 혼잡도를 조회합니다.
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
     * 현재 혼잡도 정보를 조회합니다.
     *
     * 요청 예시:
     * GET /api/crowds?latitude=37.5826&longitude=126.9810
     *
     * 위도·경도가 함께 전달되면 DB에 저장된 최신 혼잡도 데이터 중
     * 3km 이내의 최근접 서울시 핫스팟을 반환합니다.
     * 좌표가 없을 때만 기존 핫스팟 지역명 조회를 수행합니다.
     *
     * @param areaName 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소명 (optional)
     * @param latitude 조회할 장소의 위도 (optional)
     * @param longitude 조회할 장소의 경도 (optional)
     * @return 혼잡도 단계, 메시지, 예상 인구 범위, 측정 시각이 담긴 응답 DTO
     */
    @GetMapping
    public CrowdResponse getCrowdStatus(
            @RequestParam(required = false) String areaName,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        if (latitude != null || longitude != null) {
            if (latitude == null || longitude == null) {
                throw new IllegalArgumentException("혼잡도 좌표 조회에는 위도와 경도를 함께 전달해야 합니다.");
            }

            log.info("좌표 기준 혼잡도 조회 요청: latitude={}, longitude={}", latitude, longitude);
            CrowdResponse response = crowdService.getNearestCrowdStatus(latitude, longitude);
            log.info(
                    "좌표 기준 혼잡도 조회 응답: latitude={}, longitude={}, matchedAreaName={}, congestionLevel={}",
                    latitude,
                    longitude,
                    response.areaName(),
                    response.congestionLevel()
            );
            return response;
        }

        if (areaName == null || areaName.isBlank()) {
            throw new IllegalArgumentException("지역명 또는 위도·경도는 필수입니다.");
        }

        log.info("지역명 기준 혼잡도 조회 요청: areaName={}", areaName);
        CrowdResponse response = crowdService.getCrowdStatus(areaName);
        log.info("지역명 기준 혼잡도 조회 응답: areaName={}, congestionLevel={}", areaName, response.congestionLevel());
        return response;
    }

    /**
     * 현재 가장 혼잡한 서울 지역을 상위 개수만큼 조회합니다.
     *
     * 요청 예시:
     * GET /api/crowds/top?limit=10
     *
     * 지역별 최신 저장 데이터만 사용하므로 서울시 외부 API를 추가로 호출하지 않습니다.
     *
     * @param limit 반환할 최대 지역 수 (기본값 10, 최대 20)
     * @return 혼잡도 높은 순서로 정렬된 지역 목록
     */
    @GetMapping("/top")
    public List<CrowdResponse> getTopCrowdStatuses(
            @RequestParam(defaultValue = "10") int limit
    ) {
        int validatedLimit = Math.min(Math.max(limit, 1), 20);

        log.info("혼잡도 상위 지역 조회 요청: requestedLimit={}, validatedLimit={}", limit, validatedLimit);
        List<CrowdResponse> responses = crowdService.getTopCrowdStatuses(validatedLimit);
        log.info("혼잡도 상위 지역 조회 응답: resultCount={}", responses.size());

        return responses;
    }
}
