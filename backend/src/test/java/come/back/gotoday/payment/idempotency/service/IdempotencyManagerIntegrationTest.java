package come.back.gotoday.payment.idempotency.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import come.back.gotoday.payment.idempotency.enums.IdempotencyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "KMA_WEATHER_API_KEY=mock_api_key",
        "weather.kma.service-key=mock_api_key",
        "SEOUL_CROWD_AREA_NAMES=강남역,홍대입구역",
        "TOUR_API_KEY=mock_tour_api_key"
})
@ActiveProfiles("test")
@Transactional // 테스트 완료 후 실제 데이터베이스 롤백 처리
@DisplayName("IdempotencyManager 통합 테스트 (실제 DB 제약조건 검증)")
class IdempotencyManagerIntegrationTest {

    // 1. 클래스 상단에 MemberRepository 주입받기
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private IdempotencyManager idempotencyManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private jakarta.persistence.EntityManager em;

    private Member testMember; // 테스트 전체에서 사용할 유저 객체
    private final String IDEMPOTENCY_KEY = "integration-test-key-2026";
    private final String REQUEST_PATH = "/api/test/payment";
    private final String RAW_BODY = "{\"amount\":9900}";
    private Long savedMemberId;

    @BeforeEach
    void setUp() {
        // 1. JdbcTemplate으로 DB에 로우 인서트
        jdbcTemplate.update("INSERT INTO `member` (email, password, nickname, role, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "test@idempotency.com", "password", "멱등맨", "ROLE_USER", "ACTIVE", LocalDateTime.now(), LocalDateTime.now());

        // 2. 인서트된 member의 고유 ID 추출
        savedMemberId = jdbcTemplate.queryForObject("SELECT id FROM `member` WHERE email = ?", Long.class, "test@idempotency.com");

        // [핵심] 자바 객체를 새로 만드는 대신, DB에 들어간 데이터를 JPA 리포지토리로 조회해옵니다.
        // 이렇게 해야 영속성 컨텍스트(1차 캐시)에 ID가 맵핑된 채로 '영속 상태'가 됩니다.
        testMember = memberRepository.findById(savedMemberId)
                .orElseThrow(() -> new IllegalStateException("테스트 유저 생성에 실패했습니다."));
    }

    @Test
    @DisplayName("신규 요청 진입 시, 진짜 DB에 PROCESSING 상태로 정상 저장되고 조회가 가능해야 한다.")
    void it_saves_new_processing_key_into_real_db() {
        // when
        IdempotencyKey lock = idempotencyManager.getOrCreateLock(testMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY);

        // then
        assertThat(lock).isNotNull();

        // JdbcTemplate을 통해 실제 테이블 락 상태를 로우하게 조회하여 검증
        Map<String, Object> dbRow = jdbcTemplate.queryForMap(
                "SELECT * FROM `idempotency_keys` WHERE idempotency_key = ?", IDEMPOTENCY_KEY);

        assertThat(dbRow.get("status")).isEqualTo("PROCESSING");
        assertThat(dbRow.get("request_path")).isEqualTo(REQUEST_PATH);
    }

    @Test
    @DisplayName("찰나의 순간에 동일 유저가 같은 멱등키로 동시 인서트를 시도하면 UK 제약조건에 의해 차단 예외가 터져야 한다.")
    void it_blocks_concurrent_requests_by_database_unique_constraint() {
        // given - 하나의 요청이 미리 영속화되어 DB에 들어간 상태
        idempotencyManager.getOrCreateLock(testMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY);

        // when & then - 찰나의 순간에 동일 키가 try 블록의 saveAndFlush를 타면 H2 DB 유니크 제약조건(uk_idempotency_member_key)이 발동한다.
        assertThatThrownBy(() -> idempotencyManager.getOrCreateLock(testMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_PROCESSED_PAYMENT);
    }

    @Test
    @DisplayName("기존 멱등키 상태가 TIMEOUT인 경우, 실제 DB의 벌크 UPDATE 조건문을 충족하여 PROCESSING으로 안전하게 복구된다.")
    void it_successfully_recovers_timeout_key_to_processing_in_real_db() {
        // given - 타임아웃 상태의 데이터를 로우 쿼리로 밀어 넣음
        jdbcTemplate.update("INSERT INTO `idempotency_keys` (member_id, idempotency_key, request_path, request_body_hash, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                savedMemberId, IDEMPOTENCY_KEY, REQUEST_PATH, "some_hash_value", "TIMEOUT", LocalDateTime.now(), LocalDateTime.now());

        // when - 타임아웃 대상 구출 및 선점 로직 구동
        IdempotencyKey lock = idempotencyManager.getOrCreateLock(testMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY);

        // then
        assertThat(lock.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);

        String currentDbStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM `idempotency_keys` WHERE idempotency_key = ?", String.class, IDEMPOTENCY_KEY);
        assertThat(currentDbStatus).isEqualTo("PROCESSING"); // DB 상에서도 완벽히 갱신 완료
    }

    @Test
    @DisplayName("상태 변경 스펙 메서드들(Success/Fail/Timeout)이 실제 DB 레코드 컬럼값을 정확히 업데이트하는지 검증한다.")
    void it_updates_real_db_columns_when_calling_update_methods() {
        // given - 최초 PROCESSING 락 선점
        IdempotencyKey lock = idempotencyManager.getOrCreateLock(testMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY);

        // 1. SUCCESS 상태 변경 검증
        idempotencyManager.updateToSuccess(lock, 200, "{\"result\":\"OK\"}");

        // [핵심] JPA가 영속성 컨텍스트에 들고 있던 UPDATE 문을 DB에 즉시 반영합니다.
        em.flush();
        em.clear(); // 1차 캐시를 비워 깔끔한 데이터 상태 유지

        String successStatus = jdbcTemplate.queryForObject("SELECT status FROM `idempotency_keys` WHERE idempotency_key = ?", String.class, IDEMPOTENCY_KEY);
        Integer responseCode = jdbcTemplate.queryForObject("SELECT response_code FROM `idempotency_keys` WHERE idempotency_key = ?", Integer.class, IDEMPOTENCY_KEY);
        assertThat(successStatus).isEqualTo("SUCCESS");
        assertThat(responseCode).isEqualTo(200);

        // 2. TIMEOUT 상태 변경 검증
        idempotencyManager.updateToTimeout(lock, 408, "Gateway Timeout");

        //  [핵심] 한 번 더 DB 동기화
        em.flush();
        em.clear();

        String timeoutStatus = jdbcTemplate.queryForObject("SELECT status FROM `idempotency_keys` WHERE idempotency_key = ?", String.class, IDEMPOTENCY_KEY);
        assertThat(timeoutStatus).isEqualTo("TIMEOUT");
    }
}