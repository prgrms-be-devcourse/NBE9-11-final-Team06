package come.back.gotoday.course.entity;

import come.back.gotoday.event.entity.Event;
import come.back.gotoday.place.entity.Place;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "visit_order", nullable = false)
    private Integer visitOrder;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "stay_minutes")
    private Integer stayMinutes;

    @Column(name = "move_minutes_from_prev")
    private Integer moveMinutesFromPrev;

    @Column(name = "distance_from_prev")
    private Double distanceFromPrev;

    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private CoursePlace(Course course, Place place, Event event, Integer visitOrder, LocalDate visitDate,
                        LocalTime startTime, LocalTime endTime, Integer stayMinutes, Integer moveMinutesFromPrev,
                        Double distanceFromPrev, String recommendationReason) {
        this.course = course;
        this.place = place;
        this.event = event;
        this.visitOrder = visitOrder;
        this.visitDate = visitDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.stayMinutes = stayMinutes;
        this.moveMinutesFromPrev = moveMinutesFromPrev;
        this.distanceFromPrev = distanceFromPrev;
        this.recommendationReason = recommendationReason;
        this.createdAt = LocalDateTime.now();
    }

    // [규칙 반영] 정적 팩토리 메서드
    public static CoursePlace create(Course course, Place place, Event event, Integer visitOrder, LocalDate visitDate,
                                     LocalTime startTime, LocalTime endTime, Integer stayMinutes, Integer moveMinutesFromPrev,
                                     Double distanceFromPrev, String recommendationReason) {
        return new CoursePlace(course, place, event, visitOrder, visitDate, startTime, endTime, stayMinutes, moveMinutesFromPrev, distanceFromPrev, recommendationReason);
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}