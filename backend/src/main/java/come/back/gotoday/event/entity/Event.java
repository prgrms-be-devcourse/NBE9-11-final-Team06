package come.back.gotoday.event.entity;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.place.entity.Place;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "event_time", length = 100)
    private String eventTime;

    @Column(length = 100)
    private String fee;

    @Column(length = 255)
    private String target;

    @Column(name = "homepage_url", length = 500)
    private String homepageUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Event(Place place, Category category, String title, LocalDate startDate, LocalDate endDate,
                  String eventTime, String fee, String target, String homepageUrl, String imageUrl,
                  String description, String source, String externalId) {
        this.place = place;
        this.category = category;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.eventTime = eventTime;
        this.fee = fee;
        this.target = target;
        this.homepageUrl = homepageUrl;
        this.imageUrl = imageUrl;
        this.description = description;
        this.source = source;
        this.externalId = externalId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // [규칙 반영] 정적 팩토리 메서드
    public static Event create(Place place, Category category, String title, LocalDate startDate, LocalDate endDate,
                               String eventTime, String fee, String target, String homepageUrl, String imageUrl,
                               String description, String source, String externalId) {
        return new Event(place, category, title, startDate, endDate, eventTime, fee, target, homepageUrl, imageUrl, description, source, externalId);
    }
}