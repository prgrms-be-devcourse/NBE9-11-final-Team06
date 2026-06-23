package come.back.gotoday.place.entity;

import come.back.gotoday.category.entity.Category;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Place {

    public static final String TOUR_API_SOURCE = "TOUR_API";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(name = "road_address", length = 500)
    private String roadAddress;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 50)
    private String phone;

    @Column(name = "place_url", length = 500)
    private String placeUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Place(
            Category category,
            String name,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String phone,
            String placeUrl,
            String description,
            String source,
            String externalId,
            Boolean isActive
    ) {
        this.category = category;
        this.name = name;
        this.address = address;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.placeUrl = placeUrl;
        this.description = description;
        this.source = source;
        this.externalId = externalId;
        this.isActive = isActive;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Place create(
            Category category,
            String name,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String phone,
            String placeUrl,
            String description,
            String source,
            String externalId,
            Boolean isActive
    ) {
        return new Place(
                category,
                name,
                address,
                roadAddress,
                latitude,
                longitude,
                phone,
                placeUrl,
                description,
                source,
                externalId,
                isActive
        );
    }

    public static Place createTourPlace(
            Category category,
            String name,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String phone,
            String placeUrl,
            String description,
            String externalId
    ) {
        return new Place(
                category,
                name,
                address,
                roadAddress,
                latitude,
                longitude,
                phone,
                placeUrl,
                description,
                TOUR_API_SOURCE,
                externalId,
                true
        );
    }

    public void update(
            Category category,
            String name,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String phone,
            String placeUrl,
            String description,
            String externalId
    ) {
        this.category = category;
        this.name = name;
        this.address = address;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.placeUrl = placeUrl;
        this.description = description;
        this.externalId = externalId;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTourInfo(
            Category category,
            String name,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String phone,
            String placeUrl,
            String description
    ) {
        this.category = category;
        this.name = name;
        this.address = address;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.placeUrl = placeUrl;
        this.description = description;
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }
}