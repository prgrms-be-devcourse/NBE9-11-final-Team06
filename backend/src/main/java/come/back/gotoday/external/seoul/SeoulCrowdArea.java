package come.back.gotoday.external.seoul;

import java.util.Arrays;
import java.util.List;

/**
 * 서울시 실시간 도시데이터 API의 전체 혼잡도 대상 지역명을 관리합니다.
 *
 * 지역 목록은 환경 변수 또는 JVM 시스템 프로퍼티
 * {@code SEOUL_CROWD_AREA_NAMES}에 쉼표로 구분하여 설정합니다.
 * 서울시에서 제공하는 주요 장소 목록이 변경되더라도 코드 수정 없이
 * 설정값만 변경할 수 있도록 구성합니다.
 */
public final class SeoulCrowdArea {

    private static final String AREA_NAMES_PROPERTY = "SEOUL_CROWD_AREA_NAMES";

    private SeoulCrowdArea() {
    }

    /**
     * 서울시 실시간 도시데이터 API에서 제공하는 전체 혼잡도 대상 지역명을 반환합니다.
     *
     * JVM 시스템 프로퍼티를 먼저 확인하고, 값이 없으면 환경 변수를 사용합니다.
     * 지역명은 쉼표로 구분하며 공백과 빈 값은 제거합니다.
     *
     * @return 설정된 전체 지역명 목록
     * @throws IllegalStateException 전체 지역명 설정이 없거나 비어 있는 경우
     */
    public static List<String> getAllAreaNames() {
        String configuredAreaNames = System.getProperty(AREA_NAMES_PROPERTY);

        if (configuredAreaNames == null || configuredAreaNames.isBlank()) {
            configuredAreaNames = System.getenv(AREA_NAMES_PROPERTY);
        }

        if (configuredAreaNames == null || configuredAreaNames.isBlank()) {
            throw new IllegalStateException(
                    AREA_NAMES_PROPERTY
                            + " 설정이 필요합니다. 서울시 주요 장소명을 쉼표로 구분하여 입력해주세요."
            );
        }

        List<String> areaNames = Arrays.stream(configuredAreaNames.split(","))
                .map(String::trim)
                .filter(areaName -> !areaName.isBlank())
                .distinct()
                .toList();

        if (areaNames.isEmpty()) {
            throw new IllegalStateException(
                    AREA_NAMES_PROPERTY + " 설정에 유효한 지역명이 없습니다."
            );
        }

        return areaNames;
    }
}
