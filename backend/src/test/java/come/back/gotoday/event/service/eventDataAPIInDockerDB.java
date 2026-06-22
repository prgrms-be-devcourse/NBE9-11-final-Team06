package come.back.gotoday.event.service;


import come.back.gotoday.GoTodayApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = GoTodayApplication.class)
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.junit.jupiter.api.Disabled("로컬에서 도커에 직접 값을 넣을 때 사용하는 테스트 코드/ 배치를 강제로 실행")
class eventDataAPIInDockerDB {

    @Autowired
    private EventBatchService eventBatchService;

    @Test
    @DisplayName("서울시 공공 API 전체 데이터(약 400건) 동기화 및 도커 DB 적재 테스트")
    void insertAllSeoulEventsToDockerMysql() {
        // given & when
        System.out.println("=================================================");
        System.out.println("🚀 서울시 Open API 전체 적재 배치 테스트 시작 🚀");
        System.out.println("=================================================");

        // 실제 서비스를 호출하여 1000건씩 청크 단위로 허깅페이스 임베딩을 추출하고 DB에 저장합니다.
        eventBatchService.syncSeoulEvents();

        System.out.println("=================================================");
        System.out.println("✅ 전체 데이터 적재 프로세스 완료 ✅");
        System.out.println("=================================================");
    }



}
