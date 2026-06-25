package come.back.gotoday;

import org.junit.jupiter.api.Test;
import come.back.gotoday.tour.service.TourSyncService;
import come.back.gotoday.admin.service.KakaoPlaceSyncService;
import come.back.gotoday.external.weather.KmaWeatherClient;
import come.back.gotoday.course.service.CourseService;
import come.back.gotoday.external.kakao.service.KakaoLocalService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "SEOUL_CROWD_AREA_NAMES=강남역,홍대 관광특구,성수카페거리",
        "crowd.scheduler.enabled=false",
        "TOUR_API_KEY=test-tour-api-key"
})
class GoTodayApplicationTests {

    @MockitoBean
    private TourSyncService tourSyncService;

    @MockitoBean
    private KakaoPlaceSyncService kakaoPlaceSyncService;

    @MockitoBean
    private KmaWeatherClient kmaWeatherClient;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private KakaoLocalService kakaoLocalService;

    @MockitoBean(name = "apiKeyCheckConfig")
    private Object apiKeyCheckConfig;

    @Test
    void contextLoads() {
    }

}
