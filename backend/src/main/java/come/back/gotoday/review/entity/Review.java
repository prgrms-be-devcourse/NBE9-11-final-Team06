package come.back.gotoday.review.entity;

import come.back.gotoday.course.entity.Course;
import come.back.gotoday.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 유저가 리뷰를 남길때, 유저는 한 코스에 대하여
 * 한번의 리뷰를 남길 수 있도록 uniqueConstraints 조항을 추가한다 **/
@Entity
@Getter
@Table(
        name = "review",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "course_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 리뷰 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 어떤 코스 리뷰인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // 별점 (1~5점까지 존재한다)
    @Min(1)
    @Max(5)
    private Integer rating;

    // 리뷰 내용
    @Column(length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Review(Member member, Course course, Integer rating, String content) {
        this.member = member;
        this.course = course;
        this.rating = rating;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // [규칙 반영] 정적 팩토리 메서드
    public static Review create(Member member, Course course, Integer rating, String content) {
        return new Review(member, course, rating, content);
    }

    public void update(Integer rating, String content) {
        this.rating = rating;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
