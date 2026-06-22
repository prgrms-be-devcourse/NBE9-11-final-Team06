package come.back.gotoday.payment.billing.entity;

import come.back.gotoday.member.entity.Member;
import come.back.gotoday.payment.billing.config.BillingKeyConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_info",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_billing_key", columnNames = {"billingKey"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //  이 카드(빌링키) 소유자 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String customerKey; // 토스 API 요청 시 필수인 고객 식별키

    @Convert(converter = BillingKeyConverter.class)
    @Column(nullable = false, length = 512)
    private String billingKey;

    @Column(nullable = false)
    private String cardCompany; // 카드사 이름 (화면 표시용)

    @Column(nullable = false)
    private String cardNumber; // 마스킹된 카드번호 (화면 표시용)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private BillingInfo(Member member, String customerKey, String billingKey, String cardCompany, String cardNumber) {
        this.member = member;
        this.customerKey = customerKey;
        this.billingKey = billingKey;
        this.cardCompany = cardCompany;
        this.cardNumber = cardNumber;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    public static BillingInfo create(Member member, String customerKey, String billingKey, String cardCompany, String cardNumber) {
        return new BillingInfo(member, customerKey, billingKey, cardCompany, cardNumber);
    }
}