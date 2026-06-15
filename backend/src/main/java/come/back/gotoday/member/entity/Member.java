package come.back.gotoday.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_provider_provider_id",
                        columnNames = {"provider", "provider_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    private static final String STATUS_DELETED = "DELETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, length = 20)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Member(String email, String password, String nickname, String role, String status) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private Member(
            String email,
            String password,
            String nickname,
            String profileImageUrl,
            String role,
            String status,
            OAuthProvider provider,
            String providerId
    ) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
        this.status = status;
        this.provider = provider;
        this.providerId = providerId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Member create(String email, String password, String nickname, String role, String status) {
        return new Member(email, password, nickname, role, status);
    }

    public static Member createOAuthMember(
            String email,
            String nickname,
            String profileImageUrl,
            OAuthProvider provider,
            String providerId,
            String role,
            String status
    ) {
        return new Member(
                email,
                "",
                nickname,
                profileImageUrl,
                role,
                status,
                provider,
                providerId
        );
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateOAuthProfile(String nickname, String profileImageUrl) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }

        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            this.profileImageUrl = profileImageUrl;
        }

        this.updatedAt = LocalDateTime.now();
    }

    public void withdraw() {
        String deletedToken = UUID.randomUUID().toString().replace("-", "");

        this.email = "deleted_email_" + deletedToken + "@deleted.local";
        this.nickname = "탈퇴회원_" + deletedToken.substring(0, 8);
        this.profileImageUrl = null;
        this.status = STATUS_DELETED;

        if (this.providerId != null) {
            this.providerId = "deleted_provider_" + deletedToken;
        }

        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return STATUS_DELETED.equals(this.status);
    }
}