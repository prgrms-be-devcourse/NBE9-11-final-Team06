package come.back.gotoday.payment.subscription.scheduler;


import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import come.back.gotoday.payment.subscription.service.SubscriptionBatchFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@SpringBootTest(properties = {
        "KMA_WEATHER_API_KEY=mock_api_key",
        "weather.kma.service-key=mock_api_key",
        "SEOUL_CROWD_AREA_NAMES=강남역,홍대입구역",
        "TOUR_API_KEY=mock_tour_api_key"
})
@org.junit.jupiter.api.Disabled("로컬에서만 테스트 운영환경에 영향을 주기 않기 위함")
class SubscriptionBatchSchedulerTest {

    @Autowired
    private SubscriptionBatchScheduler subscriptionBatchScheduler;

    @Autowired
    private TaskExecutor schedulerTaskExecutor; // 실제 비동기 스레드 풀 주입

    @MockitoBean
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private SubscriptionBatchFacade subscriptionBatchFacade;

    @Test
    @DisplayName("정기 정산 배치 비동기 실행 및 예외 격리 테스트")
    void runAutomatedBillingPayment_AsyncSuccess() throws Exception {
        // given
        LocalDate today = LocalDate.now();

        // 테스트용 가짜 구독 데이터 5건 생성
        Subscription sub1 = createMockSubscription(1L, SubscriptionStatus.ACTIVE, today.plusMonths(1));
        Subscription sub2 = createMockSubscription(2L, SubscriptionStatus.ACTIVE, today.plusMonths(1));
        Subscription sub3 = createMockSubscription(3L, SubscriptionStatus.ACTIVE, today.plusMonths(1)); // 예외 발생 시킬 건
        Subscription sub4 = createMockSubscription(4L, SubscriptionStatus.ACTIVE, today.plusMonths(1));
        Subscription sub5 = createMockSubscription(5L, SubscriptionStatus.CANCELED_RESERVED, today.minusDays(1)); // 해지 대상 건

        List<Subscription> mockList = List.of(sub1, sub2, sub3, sub4, sub5);
        SliceImpl<Subscription> firstSlice = new SliceImpl<>(mockList, PageRequest.of(0, 100), false);
        SliceImpl<Subscription> emptySlice = new SliceImpl<>(List.of(), PageRequest.of(0, 100), false);

        // Repository 페이징 호출 정의 (첫 번째는 데이터 반환, 두 번째는 빈 값 반환하여 루프 종료)
        when(subscriptionRepository.findBillingTargets(anyLong(), any(LocalDate.class), anyList(), any(PageRequest.class)))
                .thenReturn(firstSlice)  // 1번째 호출
                .thenReturn(emptySlice);  // 2번째 호출 (while문 탈출용)

        // 특정 ID(3번) 처리 시 의도적으로 예외(RuntimeException)를 발생 시켜 예외 격리 검증
        doThrow(new RuntimeException("의도된 결제 외부 연동 실패"))
                .when(subscriptionBatchFacade).executeScheduledPayment(eq(3L));

        // when
        //  실제 배치를 실행합니다. 내부에서 schedulerTaskExecutor를 통해 멀티스레드로 처리됩니다.
        subscriptionBatchScheduler.runAutomatedBillingPayment();

        // then
        // 1. 각 메서드들이 올바르게 호출되었는지 횟수 검증 (3번이 터져도 4, 5번이 다 돌아야 함)
        verify(subscriptionBatchFacade, times(1)).executeScheduledPayment(1L);
        verify(subscriptionBatchFacade, times(1)).executeScheduledPayment(2L);
        verify(subscriptionBatchFacade, times(1)).executeScheduledPayment(3L); // 실패했어도 호출은 됨
        verify(subscriptionBatchFacade, times(1)).executeScheduledPayment(4L); // 3번이 터졌지만 비동기로 격리되어 정상 호출되어야 함
        verify(subscriptionBatchFacade, times(1)).finalizeSubscription(5L);     // 해지 예약 건도 정상 호출되어야 함
    }

    // Mock용 Subscription 엔티티를 안전하게 생성하기 위한 헬퍼 메서드
    private Subscription createMockSubscription(Long id, SubscriptionStatus status, LocalDate nextBillingDate) {
        Subscription subscription = Mockito.mock(Subscription.class);
        when(subscription.getId()).thenReturn(id);
        when(subscription.getStatus()).thenReturn(status);
        when(subscription.getNextBillingDate()).thenReturn(nextBillingDate);
        return subscription;
    }
}