package come.back.gotoday.payment.idempotency.entity;

import come.back.gotoday.member.entity.Member;
import come.back.gotoday.payment.idempotency.enums.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_idempotency_member_key",
                        columnNames = {"member_id", "idempotency_key"} // 한 유저 내에서 동일한 멱등키 중복 진입 방지
                )
        },
        indexes = {
                @Index(name = "idx_idempotency_created_at", columnList = "created_at") // 만료 배치용 인덱스
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey; // 클라이언트 헤더에서 넘어온 멱등키

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Column(name = "request_body_hash", nullable = false, length = 64)
    private String requestBodyHash; // 변조 방지 SHA-256 해시값

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status; // PROCESSING, SUCCESS, FAILED

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 내부 생성자
    private IdempotencyKey(Member member, String idempotencyKey, String requestPath, String requestBodyHash) {
        this.member = member;
        this.idempotencyKey = idempotencyKey;
        this.requestPath = requestPath;
        this.requestBodyHash = requestBodyHash;
        this.status = IdempotencyStatus.PROCESSING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 정적 팩토리 메서드
    public static IdempotencyKey startProcessing(Member member, String idempotencyKey, String requestPath, String rawBody) {
        return new IdempotencyKey(
                member,
                idempotencyKey,
                requestPath,
                hashRequestBody(rawBody)
        );
    }

    // 비즈니스 로직: 성공 처리
    public void updateSuccess(int responseCode, String responseBody) {
        this.status = IdempotencyStatus.SUCCESS;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직: 실패 처리
    public void updateFailed(int responseCode, String responseBody) {
        this.status = IdempotencyStatus.FAILED;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.updatedAt = LocalDateTime.now();
    }


    public void updateTimeout(int responseCode, String responseBody) {
        this.status = IdempotencyStatus.TIMEOUT;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.updatedAt = LocalDateTime.now();
    }

    public void startProcessingAgain(String rawBody) {
        this.status = IdempotencyStatus.PROCESSING;
        this.requestBodyHash = hashRequestBody(rawBody);
        this.updatedAt = LocalDateTime.now();
    }
    // 바디 검증 메서드
    public boolean isSameBody(String currentRawBody) {
        return this.requestBodyHash.equals(hashRequestBody(currentRawBody));
    }

    private static String hashRequestBody(String rawBody) {
        if (rawBody == null) rawBody = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 해시 생성에 실패했습니다.", e);
        }
    }
}