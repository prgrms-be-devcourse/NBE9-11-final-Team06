package come.back.gotoday.preference.entity;

import come.back.gotoday.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_preference")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "preferred_area", length = 100)
    private String preferredArea;

    @Column(name = "companion_type", length = 30)
    private String companionType;

    @Column(name = "mobility_level", length = 30)
    private String mobilityLevel;

    @Column(name = "avoid_crowded", nullable = false)
    private Boolean avoidCrowded;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private UserPreference(Member member, String preferredArea, String companionType, String mobilityLevel, Boolean avoidCrowded) {
        this.member = member;
        this.preferredArea = preferredArea;
        this.companionType = companionType;
        this.mobilityLevel = mobilityLevel;
        this.avoidCrowded = avoidCrowded;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // [규칙 반영] 정적 팩토리 메서드
    public static UserPreference create(Member member, String preferredArea, String companionType, String mobilityLevel, Boolean avoidCrowded) {
        return new UserPreference(member, preferredArea, companionType, mobilityLevel, avoidCrowded);
    }
}