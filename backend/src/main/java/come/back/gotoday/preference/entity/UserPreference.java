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

    // 사용자 1명당 선호 정보 1개
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "preferred_area", nullable = false, length = 100)
    private String preferredArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "companion_type", nullable = false, length = 30)
    private CompanionType companionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "mobility_level", nullable = false, length = 30)
    private MobilityLevel mobilityLevel;

    @Column(name = "avoid_crowded", nullable = false)
    private Boolean avoidCrowded;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private UserPreference(
            Member member,
            String preferredArea,
            CompanionType companionType,
            MobilityLevel mobilityLevel,
            Boolean avoidCrowded
    ) {
        this.member = member;
        this.preferredArea = preferredArea;
        this.companionType = companionType;
        this.mobilityLevel = mobilityLevel;
        this.avoidCrowded = avoidCrowded;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static UserPreference create(
            Member member,
            String preferredArea,
            CompanionType companionType,
            MobilityLevel mobilityLevel,
            Boolean avoidCrowded
    ) {
        return new UserPreference(
                member,
                preferredArea,
                companionType,
                mobilityLevel,
                avoidCrowded
        );
    }

    public void update(
            String preferredArea,
            CompanionType companionType,
            MobilityLevel mobilityLevel,
            Boolean avoidCrowded
    ) {
        this.preferredArea = preferredArea;
        this.companionType = companionType;
        this.mobilityLevel = mobilityLevel;
        this.avoidCrowded = avoidCrowded;
        this.updatedAt = LocalDateTime.now();
    }
}