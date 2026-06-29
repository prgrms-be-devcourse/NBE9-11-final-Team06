package come.back.gotoday.payment.history.service;

import come.back.gotoday.external.toss.TossPaymentsClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "KMA_WEATHER_API_KEY=mock_api_key",
        "weather.kma.service-key=mock_api_key",
        "SEOUL_CROWD_AREA_NAMES=강남역,홍대입구역",
        "TOUR_API_KEY=mock_tour_api_key"
})
@Transactional // 테스트 완료 후 DB를 깔끔하게 Rollback하여 격리성을 유지합니다.
@ActiveProfiles("test") // 테스트용 profile을 지정 (application-test.yml 선언 시)
public abstract class IntegrationTestSupport {

    @MockitoBean
    protected TossPaymentsClient tossPaymentsClient;

}