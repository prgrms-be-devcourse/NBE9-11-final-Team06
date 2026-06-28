package come.back.gotoday.course.entity;

import come.back.gotoday.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "course_type", nullable = false, length = 30)
    private String courseType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "base_area", length = 100)
    private String baseArea;

    @Column(name = "companion_type", length = 30)
    private String companionType;

    @Column(name = "total_distance")
    private Integer totalDistance;

    @Column(name = "estimated_time")
    private Integer estimatedTime;

    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "start_latitude")
    private Double startLatitude;

    @Column(name = "start_longitude")
    private Double startLongitude;

    @Column(nullable = false)
    private Double averageRating = 0.0;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    private Course(Member member, String title, String description, String courseType, LocalDate startDate,
                   LocalDate endDate, String baseArea, String companionType, Double startLatitude, Double startLongitude,Integer totalDistance,
                   Integer estimatedTime, String recommendationReason) {
        this.member = member;
        this.title = title;
        this.description = description;
        this.courseType = courseType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.baseArea = baseArea;
        this.companionType = companionType;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.totalDistance = totalDistance;
        this.estimatedTime = estimatedTime;
        this.recommendationReason = recommendationReason;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.averageRating = 0.0;
        this.reviewCount = 0;
    }

    // [규칙 반영] 정적 팩토리 메서드
    public static Course create(Member member, String title, String description, String courseType, LocalDate startDate,
                                LocalDate endDate, String baseArea, String companionType,Double startLatitude, Double startLongitude, Integer totalDistance,
                                Integer estimatedTime, String recommendationReason) {
        return new Course(member, title, description, courseType, startDate, endDate, baseArea, companionType, startLatitude,startLongitude,totalDistance, estimatedTime, recommendationReason);
    }


    @OneToMany(
            mappedBy = "course",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CoursePlace> coursePlaces = new ArrayList<>();

    public void addCoursePlace(CoursePlace coursePlace) {
        coursePlaces.add(coursePlace);
        coursePlace.setCourse(this);
    }
}