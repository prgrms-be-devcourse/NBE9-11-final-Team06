package come.back.gotoday.external.seoul;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 서울시 실시간 도시데이터 API의 전체 혼잡도 대상 지역명을 관리합니다.
 *
 * 지역 목록은 Spring 설정값 {@code SEOUL_CROWD_AREA_NAMES}에서 주입받으며,
 * 쉼표로 구분하여 설정합니다.
 */
@Component
public class SeoulCrowdArea {

    private final List<String> areaNames;

    public SeoulCrowdArea(
            @Value("${SEOUL_CROWD_AREA_NAMES:}") String configuredAreaNames
    ) {
        if (configuredAreaNames == null || configuredAreaNames.isBlank()) {
            throw new IllegalStateException(
                    "SEOUL_CROWD_AREA_NAMES 설정이 필요합니다. "
                            + "서울시 주요 장소명을 쉼표로 구분하여 입력해주세요."
            );
        }

        this.areaNames = Arrays.stream(configuredAreaNames.split(","))
                .map(String::trim)
                .filter(areaName -> !areaName.isBlank())
                .distinct()
                .toList();

        if (areaNames.isEmpty()) {
            throw new IllegalStateException(
                    "SEOUL_CROWD_AREA_NAMES 설정에 유효한 지역명이 없습니다."
            );
        }
    }

    /**
     * 서울시 실시간 도시데이터 API에서 제공하는 전체 혼잡도 대상 지역명을 반환합니다.
     *
     * @return 설정된 전체 지역명 목록
     */
    public List<String> getAllAreaNames() {
        return areaNames;
    }
}
