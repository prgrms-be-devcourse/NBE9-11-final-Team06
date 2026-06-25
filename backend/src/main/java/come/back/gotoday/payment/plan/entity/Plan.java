package come.back.gotoday.payment.plan.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // 요금제명 (예: "BASIC_PLAN", "PREMIUM_PLAN")

    @Column(nullable = false, length = 100)
    private String displayName; // 화면 표시용 이름 (예: "기본 멤버십", "프리미엄 멤버십")

    @Column(nullable = false)
    private Long amount; // 요금제 기본 월 결제 금액

    @Column(name = "is_active", nullable = false)
    private boolean isActive; // 현재 신규 판매/가입 가능 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Plan(String name, String displayName, Long amount) {
        validateAmount(amount);
        this.name = name;
        this.displayName = displayName;
        this.amount = amount;
        this.isActive = true; // 기본적으로 활성화 상태로 생성
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Plan create(String name, String displayName, Long amount) {
        return new Plan(name, displayName, amount);
    }

    private void validateAmount(Long amount) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("요금제 금액은 0원 이상이어야 합니다.");
        }
    }

    // 요금제 가격 변경 (기존 가입자 영향도 고려 필요)
    public void updatePrice(Long newAmount) {
        validateAmount(newAmount);
        this.amount = newAmount;
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직: 요금제 판매 중단(비활성화)
    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }
}