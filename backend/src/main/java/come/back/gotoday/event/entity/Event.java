package come.back.gotoday.event.entity;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.event.enums.EventSource;
import come.back.gotoday.place.entity.Place;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
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

    @Column(name = "homepage_url", length = 1000)
    private String homepageUrl;

    @Column(name = "image_url", length = 1000)
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

    @Column(name = "area", length = 50)
    private String area;

    @Lob
    @Column(name = "embedding_vector", columnDefinition = "LONGBLOB")
    private byte[] embeddingVectorBytes;

    private Event(Place place, Category category, String title, LocalDate startDate, LocalDate endDate,
                  String eventTime, String fee, String target, String homepageUrl, String imageUrl,
                  String description, String source, String externalId,float[] embeddingVector,String area) {
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
        setEmbeddingVector(embeddingVector);
        this.area =area;
    }

    // [규칙 반영] 정적 팩토리 메서드
    public static Event create(Place place, Category category, String title, LocalDate startDate, LocalDate endDate,
                               String eventTime, String fee, String target, String homepageUrl, String imageUrl,
                               String description, EventSource source, String externalId,float[] embeddingVector,String area) {
        return new Event(place, category, title, startDate, endDate, eventTime, fee, target, homepageUrl, imageUrl, description, source.getCode(), externalId,embeddingVector,area);
    }

    // 변경 감지용 메서드
    public void updateInfo(String title, LocalDate startDate, LocalDate endDate, String homepageUrl, String imageUrl) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.homepageUrl = homepageUrl;
        this.imageUrl = imageUrl;
    }

    // 데이터 일치 확인용 메서드
    public boolean isChanged(String title, LocalDate startDate, LocalDate endDate, String homepageUrl, String imageUrl) {
        return !this.title.equals(title) ||
                !this.startDate.equals(startDate) ||
                !this.endDate.equals(endDate) ||
                !java.util.Objects.equals(this.homepageUrl, homepageUrl) ||
                !java.util.Objects.equals(this.imageUrl, imageUrl);
    }

    // 2. 최하단에 float[] 변환 편의 메서드 추가
    public float[] getEmbeddingVector() {
        if (embeddingVectorBytes == null) return null;
        ByteBuffer buffer = ByteBuffer.wrap(embeddingVectorBytes);
        float[] vector = new float[buffer.remaining() / 4];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = buffer.getFloat();
        }
        return vector;
    }

    public void setEmbeddingVector(float[] vector) {
        if (vector == null) {
            this.embeddingVectorBytes = null;
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4);
        for (float f : vector) {
            buffer.putFloat(f);
        }
        this.embeddingVectorBytes = buffer.array();
    }
}