package come.back.gotoday.payment.subscription.scheduler;

import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import come.back.gotoday.payment.subscription.service.SubscriptionBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionReconciliationScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final SubscriptionBatchService subscriptionBatchService;

    /**
     * 메인 배치가 끝난 시점 혹은 주기적으로 실행하여
     * 타임아웃 등으로 인해 PENDING 상태로 고여있는 데이터를 구출합니다.
     */
    @EventListener// 메인 배치가 끝나고 바로 실행(정합성을 위해서 웹훅이 존재하지 않음.)
    public void reconcileMismatchedPayments(SubscriptionBatchScheduler.SubscriptionBatchCompleteEvent event) {
        log.info("[정합성 보정 배치] 미결정(PENDING) 결제 건에 대한 재조회 및 보정 작업을 시작합니다.");

        List<Subscription> stuckSubscriptions = subscriptionRepository.findAllByStatus(SubscriptionStatus.PENDING);

        log.info("[정합성 보정 배치] 타임아웃 미결정 의심 건수: {}건", stuckSubscriptions.size());

        LocalDate today = event.getBatchDate();

        for (Subscription sub : stuckSubscriptions) {
            // 메인 배치에서 생성했던 고유 규칙 orderId 복원
            String orderId = String.format("ORD-BATCH-%d-%s", sub.getId(), sub.getNextBillingDate().toString());

            try {
                // 토스 주문조회 API 호출
                TossAutomatedPaymentResponse tossStatus = tossPaymentsClient.getPaymentByOrderId(orderId);

                if (tossStatus != null && "DONE".equals(tossStatus.status())) {
                    // [Case A] 토스 측에서는 실제 결제가 완결된 상황 -> 성공 처리 보정
                    subscriptionBatchService.completeScheduledPayment(sub.getId(), orderId, tossStatus);
                    log.info("[정합성 보정 성공] 토스 결제 완료 확인 완료 -> 구독 ID: {} 정상 ACTIVE 처리", sub.getId());
                } else {
                    // [Case B] 조회가 되었으나 DONE이 아닌 경우 (READY, CANCELED 등) -> 기존 실패 함수 재활용
                    String reason = "토스 결제 미완료 상태 확인 (상태: " + (tossStatus != null ? tossStatus.status() : "UNKNOWN") + ")";

                    // 기존 handleBatchPaymentFailure 메서드 호출 (originalStatus는 안전하게 ACTIVE 대입)
                    subscriptionBatchService.handleBatchPaymentFailure(
                            sub.getId(), orderId, sub.getAmount(), reason, today, SubscriptionStatus.ACTIVE
                    );
                    log.info("[정합성 보정 완료] 토스 결제 미완료 확인 -> 구독 ID: {} 실패 및 유예 처리", sub.getId());
                }

            } catch (RestClientResponseException e) {
                // [Case C] 토스 API에서 404 Not Found를 뱉은 경우 (토스 서버에 요청조차 도달하지 않음) -> 기존 실패 함수 재활용
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    log.info("[정합성 보정 완료] 토스 내 주문 이력 없음(404) 확인 -> 구독 ID: {} 실패 및 유예 처리", sub.getId());

                    subscriptionBatchService.handleBatchPaymentFailure(
                            sub.getId(), orderId, sub.getAmount(), "네트워크 타임아웃으로 토스에 결제 요청 미도달 확인", today, SubscriptionStatus.ACTIVE
                    );
                } else {
                    // 404가 아닌 다른 HTTP 에러 코드가 온 경우 -> 데이터 오염 방지를 위해 MANUAL_CHECK 전환
                    log.error("[정합성 보정 실패] 토스 API 예외 발생으로 판정 불가 -> 구독 ID: {} 를 MANUAL_CHECK로 전환합니다.", sub.getId(), e);
                    subscriptionBatchService.markAsManualCheck(sub.getId());
                }
            } catch (Exception e) {
                // [Case D] 네트워크 일시 단절 등으로 조회 자체가 실패한 경우 -> 다음 스케줄러 루프가 처리하도록 PENDING 유지
                log.error("[정합성 보정 실패] API 네트워크 오류로 판단 유보 (다음 배치에서 재시도) -> 구독 ID: {}, 사유: {}",
                        sub.getId(), e.getMessage());
            }
        }

        log.info("[정합성 보정 배치] 모든 미결정 건 조사를 완료했습니다.");
    }
}