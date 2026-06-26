package come.back.gotoday.tour.entity;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.tour.enums.TourSource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "tour")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 관광지 카테고리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // TourAPI contentid
    @Column(name = "content_id", nullable = false, unique = true, length = 100)
    private String contentId;

    // TourAPI contenttypeid
    @Column(name = "content_type_id", length = 50)
    private String contentTypeId;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String address;

    @Column(name = "detail_address", length = 500)
    private String detailAddress;

    @Column(length = 100)
    private String tel;

    @Column(name = "homepage_url", length = 1000)
    private String homepageUrl;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(name = "area_code", length = 50)
    private String areaCode;

    @Column(name = "sigungu_code", length = 50)
    private String sigunguCode;

    @Column(length = 50)
    private String cat1;

    @Column(length = 50)
    private String cat2;

    @Column(length = 50)
    private String cat3;

    // 화면 표시 및 추천 설명에 사용하는 관광지 세부 분류명
    @Column(name = "detail_category_name", length = 50)
    private String detailCategoryName;

    // 추천 조회용 지역명 예: 성동구, 마포구
    @Column(name = "area", length = 50)
    private String area;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Lob
    @Column(name = "embedding_vector", columnDefinition = "LONGBLOB")
    private byte[] embeddingVectorBytes;

    private Tour(
            Category category,
            String contentId,
            String contentTypeId,
            String title,
            String address,
            String detailAddress,
            String tel,
            String homepageUrl,
            String imageUrl,
            String thumbnailUrl,
            String overview,
            String areaCode,
            String sigunguCode,
            String cat1,
            String cat2,
            String cat3,
            String detailCategoryName,
            String area,
            Double latitude,
            Double longitude,
            String source,
            float[] embeddingVector
    ) {
        this.category = category;
        this.contentId = contentId;
        this.contentTypeId = contentTypeId;
        this.title = title;
        this.address = address;
        this.detailAddress = detailAddress;
        this.tel = tel;
        this.homepageUrl = homepageUrl;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.overview = overview;
        this.areaCode = areaCode;
        this.sigunguCode = sigunguCode;
        this.cat1 = cat1;
        this.cat2 = cat2;
        this.cat3 = cat3;
        this.detailCategoryName = detailCategoryName;
        this.area = area;
        this.latitude = latitude;
        this.longitude = longitude;
        this.source = source;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        setEmbeddingVector(embeddingVector);
    }

    public static Tour create(
            Category category,
            String contentId,
            String contentTypeId,
            String title,
            String address,
            String detailAddress,
            String tel,
            String homepageUrl,
            String imageUrl,
            String thumbnailUrl,
            String overview,
            String areaCode,
            String sigunguCode,
            String cat1,
            String cat2,
            String cat3,
            String detailCategoryName,
            String area,
            Double latitude,
            Double longitude,
            TourSource source,
            float[] embeddingVector
    ) {
        return new Tour(
                category,
                contentId,
                contentTypeId,
                title,
                address,
                detailAddress,
                tel,
                homepageUrl,
                imageUrl,
                thumbnailUrl,
                overview,
                areaCode,
                sigunguCode,
                cat1,
                cat2,
                cat3,
                detailCategoryName,
                area,
                latitude,
                longitude,
                source.getCode(),
                embeddingVector
        );
    }

    public void updateInfo(
            Category category,
            String title,
            String address,
            String detailAddress,
            String tel,
            String homepageUrl,
            String imageUrl,
            String thumbnailUrl,
            String overview,
            String cat1,
            String cat2,
            String cat3,
            String detailCategoryName,
            String area,
            Double latitude,
            Double longitude
    ) {
        this.category = category;
        this.title = title;
        this.address = address;
        this.detailAddress = detailAddress;
        this.tel = tel;
        this.homepageUrl = homepageUrl;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.overview = overview;
        this.cat1 = cat1;
        this.cat2 = cat2;
        this.cat3 = cat3;
        this.detailCategoryName = detailCategoryName;
        this.area = area;
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isChanged(
            String title,
            String address,
            String detailAddress,
            String tel,
            String homepageUrl,
            String imageUrl,
            String thumbnailUrl,
            String overview,
            String cat1,
            String cat2,
            String cat3,
            String detailCategoryName,
            String area,
            Double latitude,
            Double longitude
    ) {
        return !Objects.equals(this.title, title) ||
                !Objects.equals(this.address, address) ||
                !Objects.equals(this.detailAddress, detailAddress) ||
                !Objects.equals(this.tel, tel) ||
                !Objects.equals(this.homepageUrl, homepageUrl) ||
                !Objects.equals(this.imageUrl, imageUrl) ||
                !Objects.equals(this.thumbnailUrl, thumbnailUrl) ||
                !Objects.equals(this.overview, overview) ||
                !Objects.equals(this.cat1, cat1) ||
                !Objects.equals(this.cat2, cat2) ||
                !Objects.equals(this.cat3, cat3) ||
                !Objects.equals(this.detailCategoryName, detailCategoryName) ||
                !Objects.equals(this.area, area) ||
                !Objects.equals(this.latitude, latitude) ||
                !Objects.equals(this.longitude, longitude);
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    public float[] getEmbeddingVector() {
        if (embeddingVectorBytes == null) {
            return null;
        }

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

        for (float value : vector) {
            buffer.putFloat(value);
        }

        this.embeddingVectorBytes = buffer.array();
    }
}