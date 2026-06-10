package come.back.gotoday.crowd.entity;

import come.back.gotoday.place.entity.Place;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "crowd_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrowdStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "area_name", nullable = false, length = 100)
    private String areaName;

    @Enumerated(EnumType.STRING)
    @Column(name = "congestion_level", nullable = false, length = 30)
    private CongestionLevel congestionLevel;

    @Column(name = "population_min")
    private Integer populationMin;

    @Column(name = "population_max")
    private Integer populationMax;

    @Column(length = 500)
    private String message;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static CrowdStatus create(
            Place place,
            String areaName,
            CongestionLevel congestionLevel,
            Integer populationMin,
            Integer populationMax,
            String message,
            LocalDateTime measuredAt
    ) {
        return new CrowdStatus(place, areaName, congestionLevel, populationMin, populationMax, message, measuredAt);
    }

    private CrowdStatus(
            Place place,
            String areaName,
            CongestionLevel congestionLevel,
            Integer populationMin,
            Integer populationMax,
            String message,
            LocalDateTime measuredAt
    ) {
        this.place = place;
        this.areaName = areaName;
        this.congestionLevel = congestionLevel;
        this.populationMin = populationMin;
        this.populationMax = populationMax;
        this.message = message;
        this.measuredAt = measuredAt;
        this.createdAt = LocalDateTime.now();
    }
}