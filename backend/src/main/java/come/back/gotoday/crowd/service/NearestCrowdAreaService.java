package come.back.gotoday.crowd.service;

import come.back.gotoday.crowd.entity.CrowdStatus;
import come.back.gotoday.crowd.entity.CongestionLevel;
import come.back.gotoday.crowd.repository.CrowdStatusRepository;
import come.back.gotoday.crowd.util.GeoDistanceCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NearestCrowdAreaService {

    private static final double MAX_MAPPING_DISTANCE_KM = 3.0;

    private final CrowdStatusRepository crowdStatusRepository;

    /**
     * 사용자 선택 장소의 좌표와 가장 가까운 혼잡도 지역을 조회합니다.
     *
     * 최대 매핑 거리보다 멀리 떨어진 지역은 부정확한 혼잡도 정보가
     * 적용되지 않도록 조회 결과에서 제외합니다.
     *
     * @param latitude 사용자 선택 장소의 위도
     * @param longitude 사용자 선택 장소의 경도
     * @return 최대 매핑 거리 안에 존재하는 최근접 혼잡도 지역
     */
    public Optional<NearestCrowdArea> findNearest(double latitude, double longitude) {
        validateCoordinate(latitude, longitude);

        return crowdStatusRepository.findLatestByArea().stream()
                .map(crowdStatus -> toNearestCrowdArea(crowdStatus, latitude, longitude))
                .filter(result -> result.distanceKm() <= MAX_MAPPING_DISTANCE_KM)
                .min(Comparator.comparingDouble(NearestCrowdArea::distanceKm));
    }

    private NearestCrowdArea toNearestCrowdArea(
            CrowdStatus crowdStatus,
            double latitude,
            double longitude
    ) {
        double distanceKm = GeoDistanceCalculator.calculateKilometers(
                latitude,
                longitude,
                crowdStatus.getLatitude(),
                crowdStatus.getLongitude()
        );

        return new NearestCrowdArea(
                crowdStatus.getId(),
                crowdStatus.getAreaName(),
                crowdStatus.getAreaCode(),
                crowdStatus.getLatitude(),
                crowdStatus.getLongitude(),
                distanceKm,
                crowdStatus.getCongestionLevel()
        );
    }

    private void validateCoordinate(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("위도는 -90 이상 90 이하여야 합니다.");
        }

        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("경도는 -180 이상 180 이하여야 합니다.");
        }
    }

    public record NearestCrowdArea(
            Long crowdStatusId,
            String areaName,
            String areaCode,
            double latitude,
            double longitude,
            double distanceKm,
            CongestionLevel congestionLevel
    ) {
    }
}
