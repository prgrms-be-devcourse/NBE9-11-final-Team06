package come.back.gotoday.crowd.entity;

import come.back.gotoday.place.entity.Place;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 특정 장소 또는 서울시 핫스팟의 혼잡도 정보를 저장하는 엔티티입니다.
 *
 * 서울시 실시간 도시데이터 API에서 받은 혼잡도, 예상 인구 범위,
 * 혼잡도 메시지, 측정 시각을 저장해두고 이후 추천 점수 계산이나 캐싱에 활용할 수 있습니다.
 */
@Entity
@Table(
        name = "crowd_status",
        indexes = {
                @Index(name = "idx_area_name_created_at", columnList = "area_name, created_at DESC"),
                @Index(
                        name = "idx_crowd_status_area_measured_at",
                        columnList = "area_name, measured_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrowdStatus {

    /** 혼잡도 데이터의 기본 키입니다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 혼잡도 정보가 연결될 장소입니다.
     *
     * 서울시 핫스팟명만으로 조회하는 경우에는 아직 Place와 연결되지 않을 수 있으므로 null을 허용합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    /** 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소명입니다. */
    @Column(name = "area_name", nullable = false, length = 100)
    private String areaName;

    /** 서울시 실시간 도시데이터 API에서 사용하는 핫스팟 장소 코드입니다. */
    @Column(name = "area_code", length = 30)
    private String areaCode;

    /** 서울시 실시간 도시데이터 API에서 제공하는 핫스팟 위도입니다. */
    @Column(name = "latitude")
    private Double latitude;

    /** 서울시 실시간 도시데이터 API에서 제공하는 핫스팟 경도입니다. */
    @Column(name = "longitude")
    private Double longitude;

    /** 서울시 API의 한글 혼잡도 값을 우리 서비스 enum으로 변환해 저장합니다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "congestion_level", nullable = false, length = 30)
    private CongestionLevel congestionLevel;

    /** 실시간 인구 지표 최소값입니다. */
    @Column(name = "population_min")
    private Integer populationMin;

    /** 실시간 인구 지표 최대값입니다. */
    @Column(name = "population_max")
    private Integer populationMax;

    /** 서울시 API에서 제공하는 혼잡도 안내 메시지입니다. */
    @Column(length = 500)
    private String message;

    /** 서울시 API 기준 혼잡도 데이터가 측정 또는 업데이트된 시각입니다. */
    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    /** 해당 혼잡도 데이터가 우리 DB에 저장된 시각입니다. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * CrowdStatus 엔티티 생성을 위한 정적 팩토리 메서드입니다.
     *
     * 생성자를 직접 열어두지 않고 create 메서드를 통해 생성하면
     * 객체 생성 의도를 명확하게 표현할 수 있습니다.
     */
    public static CrowdStatus create(
            Place place,
            String areaName,
            String areaCode,
            Double latitude,
            Double longitude,
            CongestionLevel congestionLevel,
            Integer populationMin,
            Integer populationMax,
            String message,
            LocalDateTime measuredAt
    ) {
        return new CrowdStatus(
                place,
                areaName,
                areaCode,
                latitude,
                longitude,
                congestionLevel,
                populationMin,
                populationMax,
                message,
                measuredAt
        );
    }

    /**
     * 실제 필드 값을 세팅하는 private 생성자입니다.
     *
     * 외부에서는 create 메서드만 사용하도록 제한하고,
     * JPA를 위한 기본 생성자는 protected로만 열어둡니다.
     */
    private CrowdStatus(
            Place place,
            String areaName,
            String areaCode,
            Double latitude,
            Double longitude,
            CongestionLevel congestionLevel,
            Integer populationMin,
            Integer populationMax,
            String message,
            LocalDateTime measuredAt
    ) {
        this.place = place;
        this.areaName = areaName;
        this.areaCode = areaCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.congestionLevel = congestionLevel;
        this.populationMin = populationMin;
        this.populationMax = populationMax;
        this.message = message;
        this.measuredAt = measuredAt;
        this.createdAt = LocalDateTime.now();
    }
}