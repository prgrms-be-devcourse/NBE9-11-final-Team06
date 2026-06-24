package come.back.gotoday.payment.plan.service;

import come.back.gotoday.payment.plan.dto.PlanResponse;
import come.back.gotoday.payment.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;

    public List<PlanResponse> getActivePlans() {
        return planRepository.findAllByIsActiveTrue().stream()
                .map(PlanResponse::from)
                .collect(Collectors.toList());
    }
}