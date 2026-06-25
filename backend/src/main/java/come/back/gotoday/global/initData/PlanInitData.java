package come.back.gotoday.global.initData;

import come.back.gotoday.payment.plan.entity.Plan;
import come.back.gotoday.payment.plan.repository.PlanRepository; // 실제 프로젝트 패키지에 맞게 수정하세요.
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PlanInitData {

    private final PlanRepository planRepository;

    @Bean
    public ApplicationRunner initPlan() {
        return args -> {
            log.info("요금제(Plan) 초기 데이터 생성 확인 시작");

            // 데이터 중복 방지: 이미 테이블에 데이터가 존재하는지 확인
            long existingPlanCount = planRepository.count();
            if (existingPlanCount > 0) {
                log.info("요금제 초기 데이터가 이미 존재하여 생성을 건너뜁니다. existingCount={}", existingPlanCount);
                return;
            }

            planRepository.save(Plan.create("BASIC_PLAN", "기본 멤버십", 4900L));
            planRepository.save(Plan.create("PREMIUM_PLAN", "프리미엄 멤버십", 9900L));

            log.info("요금제 초기 데이터 생성 완료. createdCount={}", planRepository.count());
        };
    }
}