package come.back.gotoday;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "SEOUL_CROWD_AREA_NAMES=강남역,홍대 관광특구,성수카페거리",
        "crowd.scheduler.enabled=false"
})
class GoTodayApplicationTests {

    @Test
    void contextLoads() {
    }

}
