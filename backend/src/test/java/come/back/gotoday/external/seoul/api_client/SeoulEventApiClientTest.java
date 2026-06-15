package come.back.gotoday.external.seoul.api_client;

import come.back.gotoday.external.seoul.dto.SeoulEventResponse;
import come.back.gotoday.external.seoul.service.SeoulEventAnalyzeService;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;


import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
@org.junit.jupiter.api.Disabled("로컬에서 시각적 확인을 할 때만 수동으로 실행하는 라이브 테스트")
class SeoulEventApiClientTest {

    @Autowired
    private SeoulEventApiClient seoulEventApiClient;


    // ==========================================
    // 1. 정상 호출 및 시각적 검증 테스트 (기존 유지 및 개선)
    // ==========================================

    @Autowired
    private SeoulEventAnalyzeService seoulEventAnalyzeService;

    @Test
    void 서울시_모든_코드네임_조회_테스트() {
        // when
        Set<String> codeNames = seoulEventAnalyzeService.getAllUniqueCodeNames();

        // then
        assertThat(codeNames).isNotEmpty();
        System.out.println("결과 개수: " + codeNames.size());
    }

    @Test
    @DisplayName("정상 호출 테스트 - 데이터 구조를 JSON Pretty Print로 콘솔에 출력")
    void 서울시_문화행사_API_호출_및_시각적_확인_테스트() throws Exception {
        // given
        int startIndex = 1;
        int endIndex = 5;

        // when
        SeoulEventResponse response = seoulEventApiClient.fetchEvents(startIndex, endIndex);

        // then
        assertThat(response).isNotNull();
        assertThat(response.culturalEventInfo()).isNotNull();

        List<SeoulEventResponse.EventRow> rows = response.culturalEventInfo().row();
        assertThat(rows).isNotEmpty();

        // [시각적 확인 1] 전체 데이터 구조 이쁘게 출력하기
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);

        System.out.println("\n==================== [서울시 API 응답 데이터 전체 구조] ====================");
        System.out.println(jsonResponse);
        System.out.println("====================================================================\n");

        // [시각적 확인 2] 고유 ID 검증 및 중복 체크 출력
        System.out.println("==================== [고유 ID 구분 및 중복 검증] ====================");
        List<String> externalIds = rows.stream().map(SeoulEventResponse.EventRow::externalId).toList();
        Set<String> uniqueExternalIds = rows.stream().map(SeoulEventResponse.EventRow::externalId).collect(java.util.stream.Collectors.toSet());

        rows.forEach(row ->
                System.out.printf("행사명: [%s] -> 생성된 고유 ID: [%s]\n", row.title(), row.externalId())
        );

        System.out.println("--------------------------------------------------------------------");
        System.out.println("요청한 데이터 개수: " + rows.size() + "개");
        System.out.println("추출된 고유 ID 개수: " + externalIds.size() + "개");
        System.out.println("중복 제거된 고유 ID 개수: " + uniqueExternalIds.size() + "개");
        System.out.println("====================================================================\n");

        assertThat(externalIds.size()).isEqualTo(uniqueExternalIds.size());
    }


    // ==========================================
    // 2. 비즈니스 예외 및 에러 처리 검증 테스트 (신규 추가)
    // ==========================================

    @Test
    @DisplayName("데이터 없음 예외 테스트 - 시작 인덱스가 전체 개수보다 아득히 클 때 빈 객체(empty)를 반환하는가")
    void 서울시_API_조회_데이터_없음_INFO_200_테스트() {
        // given - 서울시 데이터 범위를 벗어나는 말도 안 되게 큰 인덱스 지정
        int startIndex = 999999;
        int endIndex = 1000000;

        System.out.println("==================== [테스트: 데이터 없음 (INFO-200)] ====================");

        // when
        SeoulEventResponse response = seoulEventApiClient.fetchEvents(startIndex, endIndex);

        // then
        assertThat(response).isNotNull();
        // 비어있는 객체가 제대로 리턴되었는지 검증 (SeoulEventResponse.empty()가 정상 작동했는지 확인)
        assertThat(response.culturalEventInfo()).isNotNull(); // 객체 자체는 존재해야 함
        assertThat(response.culturalEventInfo().listTotalCount()).isEqualTo(0); // 카운트는 0이어야 함
        assertThat(response.culturalEventInfo().row()).isEmpty(); // 데이터 리스트는 비어있어야 함

        System.out.println("결과: 데이터가 없으므로 NullPointerException 방지를 위해 빈 객체가 정상 반환되었습니다.");
        System.out.println("====================================================================\n");
    }

    @Test
    @DisplayName("잘못된 요청 에러 테스트 - 시작 인덱스가 종료 인덱스보다 클 때 EXTERNAL_API_ERROR 예외를 던지는가")
    void 서울시_API_비즈니스_에러_요청_인덱스_오류_테스트() {
        // given - 시작 번호가 끝 번호보다 큰 잘못된 요청 값 설정 (서울시가 에러 결과 RESULT를 뱉음)
        int startIndex = 10;
        int endIndex = 1;

        System.out.println("==================== [테스트: 서울시 비즈니스 에러 (인덱스 역전)] ====================");

        // when & then
        assertThatThrownBy(() -> seoulEventApiClient.fetchEvents(startIndex, endIndex))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    // 예외 코드가 EXTERNAL_API_ERROR 인지 검증
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
                });

        System.out.println("결과: 서울시 자체 비즈니스 에러를 감지하고 훌륭하게 BusinessException(EXTERNAL_API_ERROR)을 발생시켰습니다.");
        System.out.println("====================================================================\n");
    }

    @Test
    @DisplayName("API Key 인증 에러 테스트 - 잘못된 API Key가 들어왔을 때 외부 에러 예외를 던지는가")
    void 서울시_API_비즈니스_에러_인증_실패_테스트() {
        System.out.println("==================== [테스트: 서울시 비즈니스 에러 (인증 실패)] ====================");

        // 실제 주입받은 Bean 대신, 생성자를 통해 강제로 잘못된 API Key를 꽂은 임시 클라이언트 객체 생성
        ObjectMapper mockMapper = new ObjectMapper();
        SeoulEventApiClient invalidKeyClient = new SeoulEventApiClient("INVALID_KEY_12345", mockMapper);

        // when & then
        assertThatThrownBy(() -> invalidKeyClient.fetchEvents(1, 5))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
                });

        System.out.println("결과: 잘못된 인증키 요청에 대해 서울시 에러 구조를 파싱하여 예외 처리에 성공했습니다.");
        System.out.println("====================================================================\n");
    }


}