package come.back.gotoday.payment.plan.service;

import come.back.gotoday.payment.plan.entity.Plan;
import come.back.gotoday.payment.plan.dto.PlanResponse;
import come.back.gotoday.payment.plan.repository.PlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanService 비즈니스 로직 단위 테스트")
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanService planService;

    @Test
    @DisplayName("케이스 1: [정상 조회] 활성화된 플랜 목록이 존재할 때, 가공된 DTO 리스트를 정확하게 반환한다.")
    void getActivePlans_Success_ReturnPlanResponseList() {
        // given
        // 💡 엔티티에 정의된 정적 팩토리 메서드(Plan.create)를 사용하여 도메인 객체를 생성합니다.
        Plan basicPlan = Plan.create("BASIC_PLAN", "기본 멤버십", 9900L);
        Plan premiumPlan = Plan.create("PREMIUM_PLAN", "프리미엄 멤버십", 14900L);

        // 💡 데이터베이스가 부여하는 고유 ID(@Id)는 가짜로 매핑하기 위해 ReflectionTestUtils를 활용합니다.
        ReflectionTestUtils.setField(basicPlan, "id", 1L);
        ReflectionTestUtils.setField(premiumPlan, "id", 2L);

        List<Plan> mockActivePlans = List.of(basicPlan, premiumPlan);
        given(planRepository.findAllByIsActiveTrue()).willReturn(mockActivePlans);

        // when
        List<PlanResponse> result = planService.getActivePlans();

        // then
        assertThat(result).hasSize(2);

        // 첫 번째 데이터 검증
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("BASIC_PLAN");
        assertThat(result.get(0).displayName()).isEqualTo("기본 멤버십");
        assertThat(result.get(0).amount()).isEqualTo(9900L);

        // 두 번째 데이터 검증
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).displayName()).isEqualTo("프리미엄 멤버십");
        assertThat(result.get(1).amount()).isEqualTo(14900L);

        verify(planRepository, times(1)).findAllByIsActiveTrue();
    }

    @Test
    @DisplayName("케이스 2: [빈 목록 조회] 활성화된 플랜이 데이터베이스에 하나도 없을 때, 에러를 내지 않고 안전하게 빈 리스트를 반환한다.")
    void getActivePlans_EmptyList_ReturnEmptyList() {
        // given
        given(planRepository.findAllByIsActiveTrue()).willReturn(List.of());

        // when
        List<PlanResponse> result = planService.getActivePlans();

        // then
        assertThat(result)
                .isNotNull()
                .isEmpty();

        verify(planRepository, times(1)).findAllByIsActiveTrue();
    }

    @Test
    @DisplayName("케이스 3: [도메인 예외 검증] 요금제 생성 시 금액이 음수이면 생성에 실패하며 IllegalArgumentException이 발생한다.")
    void createPlan_NegativeAmount_ThrowIllegalArgumentException() {
        // when & then
        assertThatThrownBy(() -> Plan.create("FREE_PLAN", "잘못된 요금제", -100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("요금제 금액은 0원 이상이어야 합니다.");
    }
}