package come.back.gotoday.course.entity;

import come.back.gotoday.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "saved_course",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_saved_course_member_course",
                        columnNames = {"member_id", "course_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private SavedCourse(Member member, Course course, String memo) {
        this.member = member;
        this.course = course;
        this.memo = memo;
        this.createdAt = LocalDateTime.now();
    }

    public static SavedCourse create(Member member, Course course) {
        return new SavedCourse(member, course, null);
    }

    public static SavedCourse create(Member member, Course course, String memo) {
        return new SavedCourse(member, course, memo);
    }
}