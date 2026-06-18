package come.back.gotoday.event.service;

import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.external.seoul.api_client.SeoulEventApiClient;
import come.back.gotoday.external.seoul.dto.SeoulEventResponse;
import come.back.gotoday.external.seoul.dto.SeoulEventResponse.CulturalEventInfo;
import come.back.gotoday.external.seoul.dto.SeoulEventResponse.EventRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties = {
        "SEOUL_CROWD_AREA_NAMES=강남역,홍대 관광특구,성수카페거리",
        "crowd.scheduler.enabled=false"
})
@Transactional
class EventBatchServiceTest {

    @Autowired
    private EventBatchService eventBatchService;

    @Autowired
    private EventRepository eventRepository;

    @MockitoBean
    private SeoulEventApiClient apiClient; // 외부 API 호출을 격리하기 위해 가짜 빈으로 등록

    @Autowired
    private jakarta.persistence.EntityManager em;


    @Test
    @DisplayName("종료일이 오늘보다 이전인 과거의 이벤트는 DB에 저장되지 않고 스킵되어야 한다")
    void 과거_이벤트_필터링_테스트() {
        // given
        LocalDate today = LocalDate.now();
        String pastStartDateStr = today.minusDays(5) + " 00:00:00.0";
        String pastEndDateStr = today.minusDays(1) + " 00:00:00.0"; // 어제 종료됨

        EventRow pastEventRow = createMockRow("과거공연", "뮤지컬/오페라", pastStartDateStr, pastEndDateStr);
        String expectedExtId = pastEventRow.externalId();

        CulturalEventInfo info = new CulturalEventInfo(1, List.of(pastEventRow));
        given(apiClient.fetchEvents(1, 1000)).willReturn(new SeoulEventResponse(info));

        // when
        eventBatchService.syncSeoulEvents();

        // then
        Optional<Event> savedEvent = eventRepository.findByExternalId(expectedExtId);
        assertThat(savedEvent).isEmpty(); // 스킵되었으므로 조회되면 안 됨
    }


    @Test
    @DisplayName("오늘 이후에 진행되는 새로운 행사는 DB에 정상적으로 INSERT 되어야 한다")
    void 신규_이벤트_저장_테스트() {
        // given
        LocalDate today = LocalDate.now();
        String currentStartDateStr = today + " 00:00:00.0";
        String futureEndDateStr = today.plusDays(5) + " 00:00:00.0"; // 오늘부터 5일간 진행

        EventRow newEventRow = createMockRow("새로운클래식", "클래식", currentStartDateStr, futureEndDateStr);
        String expectedExtId = newEventRow.externalId();

        CulturalEventInfo info = new CulturalEventInfo(1, List.of(newEventRow));
        given(apiClient.fetchEvents(1, 1000)).willReturn(new SeoulEventResponse(info));

        // when
        eventBatchService.syncSeoulEvents();

        // then
        Optional<Event> savedEvent = eventRepository.findByExternalId(expectedExtId);
        assertThat(savedEvent).isPresent();
        assertThat(savedEvent.get().getTitle()).isEqualTo("새로운클래식");
        assertThat(savedEvent.get().getStartDate()).isEqualTo(today);
        assertThat(savedEvent.get().getEndDate()).isEqualTo(today.plusDays(5));
    }


    @Test
    @DisplayName("이미 가동되어 저장된 행사의 종료 일정이나 상세 정보가 변경되면 기존 데이터가 정상적으로 UPDATE 되어야 한다")
    void 기존_이벤트_업데이트_테스트() {
        // given
        LocalDate today = LocalDate.now();
        String currentStartDateStr = today + " 00:00:00.0";
        String initialEndDateStr = today.plusDays(2) + " 00:00:00.0";

        // 1. 초기 데이터 수동 동기화 밀어넣기
        // externalId가 깨지지 않도록 '원래공연제목'을 기준으로 키를 생성하고 저장합니다.
        EventRow firstRow = createMockRow("원래공연제목", "연극", currentStartDateStr, initialEndDateStr);
        String expectedExtId = firstRow.externalId();

        given(apiClient.fetchEvents(1, 1000))
                .willReturn(new SeoulEventResponse(new CulturalEventInfo(1, List.of(firstRow))));
        eventBatchService.syncSeoulEvents();


        // 2. 외부 API 스펙에서 '제목(TITLE)'과 '시작일'은 유지하고, '종료일' 및 '참조 링크/이미지'를 수정한 레코드 생성
        String changedEndDateStr = today.plusDays(10) + " 00:00:00.0"; // 2일 뒤 종료 -> 10일 뒤 종료로 일정 변경
        EventRow updatedRow = new EventRow(
                "원래공연제목",
                "연극",
                currentStartDateStr,   //  시작일 유지 (externalId의 유일성 유지)
                changedEndDateStr,     //  [변경] 종료 일정 확장
                "종로구", "세종문화회관",
                "http://new-link.com", //  [변경] 공식 홈페이지 도메인 정정
                "http://new-img.com",  //  [변경] 새로운 포스터 포맷 이미지
                "전체", "유료", 127.0, 37.0, "15:00"
        );

        given(apiClient.fetchEvents(1, 1000))
                .willReturn(new SeoulEventResponse(new CulturalEventInfo(1, List.of(updatedRow))));

        // when
        eventBatchService.syncSeoulEvents(); // 두 번째 배치 가동 (기존 데이터 식별 후 updateInfo() 실행 유도)


        // then
        // 원래의 고유 externalId 키를 통해 DB에서 가공이 끝난 최종 엔티티를 획득합니다.
        Event finalEvent = eventRepository.findByExternalId(expectedExtId)
                .orElseThrow(() -> new AssertionError("업데이트 대상 엔티티를 찾지 못했습니다."));

        // 식별용 필드는 고정되어 있고, 비즈니스 가변 정보들만 실시간 갱신되었는지 완전 정밀 검증
        assertThat(finalEvent.getTitle()).isEqualTo("원래공연제목");
        assertThat(finalEvent.getEndDate()).isEqualTo(today.plusDays(10)); // 일정 변경 내역 반영 확인
        assertThat(finalEvent.getHomepageUrl()).isEqualTo("http://new-link.com"); // URL 변경 내역 반영 확인
        assertThat(finalEvent.getImageUrl()).isEqualTo("http://new-img.com"); // 이미지 변경 내역 반영 확인
    }


    @Test
    @DisplayName("기존에 적재된 행사와 외부 API 응답 데이터가 완벽하게 일치하면 정보 업데이트를 스킵한다")
    void 데이터_일치시_업데이트_쿼리_스킵_검증() {
        // given
        LocalDate today = LocalDate.now();
        String currentStartDateStr = today + " 00:00:00.0";
        String futureEndDateStr = today.plusDays(2) + " 00:00:00.0";

        EventRow stableRow = createMockRow("변함없는콘서트", "콘서트", currentStartDateStr, futureEndDateStr);
        String expectedExtId = stableRow.externalId();

        given(apiClient.fetchEvents(1, 1000)).willReturn(new SeoulEventResponse(new CulturalEventInfo(1, List.of(stableRow))));

        // 1. 최초 저장
        eventBatchService.syncSeoulEvents();
        Event savedBefore = eventRepository.findByExternalId(expectedExtId).orElseThrow();
        LocalDateTime firstUpdatedAt = savedBefore.getUpdatedAt();

        // 2. 완벽히 동일한 데이터로 다시 호출
        given(apiClient.fetchEvents(1, 1000)).willReturn(new SeoulEventResponse(new CulturalEventInfo(1, List.of(stableRow))));

        // when
        eventBatchService.syncSeoulEvents();

        // then
        Event savedAfter = eventRepository.findByExternalId(expectedExtId).orElseThrow();

        // 정보가 바뀌지 않았으므로 updateInfo가 트리거되지 않아 updatedAt 수정시간이 과거 시간 그대로 멈춰있어야 정상
        assertThat(savedAfter.getUpdatedAt()).isEqualTo(firstUpdatedAt);
    }

    @Test
    @DisplayName("기존에 적재되어 있던 행사 중 종료일이 오늘보다 이전인 데이터는 배치가 시작될 때 자동으로 삭제되어야 한다")
    void 기존_이벤트_만료_삭제_테스트() {
        // given
        LocalDate today = LocalDate.now();
        String currentStartDateStr = today + " 00:00:00.0";
        String futureEndDateStr = today.plusDays(2) + " 00:00:00.0"; // ️ 일단 둘 다 미래 날짜로 줍니다.

        // 1. 배치의 필터링을 통과시키기 위해, 일단 둘 다 정상적인 미래 행사로 API 응답을 만듭니다.
        EventRow expiredRow = createMockRow("만료될공연", "뮤지컬/오페라", currentStartDateStr, futureEndDateStr);
        String expiredExtId = expiredRow.externalId();

        EventRow activeRow = createMockRow("진행중인공연", "연극", currentStartDateStr, futureEndDateStr);
        String activeExtId = activeRow.externalId();

        given(apiClient.fetchEvents(1, 1000))
                .willReturn(new SeoulEventResponse(new CulturalEventInfo(2, List.of(expiredRow, activeRow))));

        // 1차 저장 배치 가동 (둘 다 정상적이므로 무조건 2건이 DB에 깔립니다)
        eventBatchService.syncSeoulEvents();

        em.flush();
        em.clear();

        // 2. [핵심] DB에 정상 저장된 '만료될공연'을 강제로 '과거(어제 종류)' 데이터로 변조합니다.
        Event eventToExpire = eventRepository.findByExternalId(expiredExtId)
                .orElseThrow(() -> new AssertionError("최초 저장이 실패했습니다."));

        // updateInfo() 메서드를 활용하거나 엔티티 상태를 강제로 바꿉니다.
        // (만약 엔티티 메서드로 안 된다면 JPQL 벌크성 쿼리로 바꾸셔도 됩니다)
        eventToExpire.updateInfo(eventToExpire.getTitle(), eventToExpire.getStartDate(), today.minusDays(1), eventToExpire.getHomepageUrl(), eventToExpire.getImageUrl(),eventToExpire.getLatitude(),eventToExpire.getLongitude());

        em.flush();
        em.clear();

        // 변조 후 두 데이터가 DB에 확실히 잘 존재하는지 1차 확인합니다.
        assertThat(eventRepository.findByExternalId(expiredExtId)).isPresent();
        assertThat(eventRepository.findByExternalId(activeExtId)).isPresent();

        // 3. 이제 다음 날 새벽 배치가 다시 도는 상황을 시뮬레이션합니다. (API 결과는 비워둠)
        given(apiClient.fetchEvents(1, 1000)).willReturn(SeoulEventResponse.empty());

        // when
        eventBatchService.syncSeoulEvents(); // 두 번째 배치 가동 (삭제 실행 단계 진입)

        em.flush();
        em.clear();

        // then
        // 케이스 A (과거로 변조된 건): cleanExpiredEvents()에 의해 지워져서 비어있어야(isEmpty) 합니다.
        Optional<Event> deletedEvent = eventRepository.findByExternalId(expiredExtId);
        assertThat(deletedEvent).isEmpty();

        // 케이스 B (미래 상태 유지 건): 안전하게 살아남아 있어야 합니다.
        Optional<Event> survivedEvent = eventRepository.findByExternalId(activeExtId);
        assertThat(survivedEvent).isPresent();
        assertThat(survivedEvent.get().getTitle()).isEqualTo("진행중인공연");
    }
    // -------------------------------------------------------------------------
    // 공통 Mock Row 생성 헬퍼 함수
    // -------------------------------------------------------------------------
    private EventRow createMockRow(String title, String codeName, String startStr, String endStr) {
        return new EventRow(
                title,
                codeName,
                startStr,
                endStr,
                "마포구",
                "홍대 상상마당",
                "http://example.com/link",
                "http://example.com/img.png",
                "누구나",
                "무료",
                126.924,
                37.551,
                "19:00-21:00"
        );
    }
}