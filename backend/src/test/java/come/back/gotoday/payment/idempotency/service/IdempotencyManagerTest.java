package come.back.gotoday.payment.idempotency.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import come.back.gotoday.payment.idempotency.enums.IdempotencyStatus;
import come.back.gotoday.payment.idempotency.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyManager 단위 테스트")
class IdempotencyManagerTest {

    @InjectMocks
    private IdempotencyManager idempotencyManager;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private Member mockMember;

    private final String IDEMPOTENCY_KEY = "test-idempotency-key-2026";
    private final String REQUEST_PATH = "/api/v1/payments/charge";
    private final String RAW_BODY = "{\"amount\":9900,\"planId\":1}";

    @Nested
    @DisplayName("getOrCreateLock 메서드는")
    class Describe_getOrCreateLock {

        @Nested
        @DisplayName("이전에 유입된 적이 없는 신규 멱등키 요청인 경우")
        class Context_with_new_idempotency_key {

            @Test
            @DisplayName("PROCESSING 상태의 새로운 IdempotencyKey를 생성하여 즉시 커밋(saveAndFlush) 후 반환한다.")
            void it_saves_and_returns_new_processing_key() {
                // given
                when(idempotencyKeyRepository.findByMemberAndIdempotencyKey(mockMember, IDEMPOTENCY_KEY))
                        .thenReturn(Optional.empty());

                // saveAndFlush 처리 시 들어오는 임의의 엔티티 객체를 그대로 가로채 반환하도록 아규먼트 매처 활용
                when(idempotencyKeyRepository.saveAndFlush(any(IdempotencyKey.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                // when
                IdempotencyKey result = idempotencyManager.getOrCreateLock(mockMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
                assertThat(result.getRequestPath()).isEqualTo(REQUEST_PATH);
                verify(idempotencyKeyRepository, times(1)).saveAndFlush(any(IdempotencyKey.class));
            }

            @Test
            @DisplayName("찰나의 순간에 동일 키가 동시 진입하여 DB 유니크 제약조건 예외가 터지면 ALREADY_PROCESSED_PAYMENT 예외를 발생시킨다.")
            void it_throws_already_processed_exception_when_db_unique_constraint_violated() {
                // given
                when(idempotencyKeyRepository.findByMemberAndIdempotencyKey(mockMember, IDEMPOTENCY_KEY))
                        .thenReturn(Optional.empty());

                // saveAndFlush 시점에 DataIntegrityViolationException 강제 유발 (try-catch 블록 검증)
                when(idempotencyKeyRepository.saveAndFlush(any(IdempotencyKey.class)))
                        .thenThrow(new DataIntegrityViolationException("Unique index or primary key violation"));

                // when & then
                assertThatThrownBy(() -> idempotencyManager.getOrCreateLock(mockMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_PROCESSED_PAYMENT);
            }
        }

        @Nested
        @DisplayName("이미 존재하는 멱등키이며 상태가 SUCCESS(성공 종결)인 경우")
        class Context_with_existing_success_key {

            @Test
            @DisplayName("더 이상 로직을 진행하지 않고 기존 멱등키 엔티티를 Early Return한다.")
            void it_returns_existing_success_key_immediately() {
                // given
                IdempotencyKey mockExistingKey = mock(IdempotencyKey.class);
                when(mockExistingKey.getStatus()).thenReturn(IdempotencyStatus.SUCCESS);

                when(idempotencyKeyRepository.findByMemberAndIdempotencyKey(mockMember, IDEMPOTENCY_KEY))
                        .thenReturn(Optional.of(mockExistingKey));

                // when
                IdempotencyKey result = idempotencyManager.getOrCreateLock(mockMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY);

                // then
                assertThat(result).isEqualTo(mockExistingKey);
                verify(idempotencyKeyRepository, never()).saveAndFlush(any());
            }
        }

        @Nested
        @DisplayName("이미 존재하는 멱등키이며 상태가 PROCESSING(처리 중)인 경우")
        class Context_with_existing_processing_key {

            @Test
            @DisplayName("연타 진입으로 판단하여 ALREADY_PROCESSED_PAYMENT 예외를 발생시킨다.")
            void it_throws_already_processed_exception() {
                // given
                IdempotencyKey mockExistingKey = mock(IdempotencyKey.class);
                when(mockExistingKey.getStatus()).thenReturn(IdempotencyStatus.PROCESSING);

                when(idempotencyKeyRepository.findByMemberAndIdempotencyKey(mockMember, IDEMPOTENCY_KEY))
                        .thenReturn(Optional.of(mockExistingKey));

                // when & then
                assertThatThrownBy(() -> idempotencyManager.getOrCreateLock(mockMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_PROCESSED_PAYMENT);
            }
        }

        @Nested
        @DisplayName("이미 존재하는 멱등키이며 상태가 FAILED(실패 고정)인 경우")
        class Context_with_existing_failed_key {

            @Test
            @DisplayName("동일 키 재진입을 허용하지 않고 ALREADY_PROCESSED_PAYMENT 예외를 발생시킨다.")
            void it_throws_already_processed_exception_for_failed_status() {
                // given
                IdempotencyKey mockExistingKey = mock(IdempotencyKey.class);
                when(mockExistingKey.getStatus()).thenReturn(IdempotencyStatus.FAILED);

                when(idempotencyKeyRepository.findByMemberAndIdempotencyKey(mockMember, IDEMPOTENCY_KEY))
                        .thenReturn(Optional.of(mockExistingKey));

                // when & then
                assertThatThrownBy(() -> idempotencyManager.getOrCreateLock(mockMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_PROCESSED_PAYMENT);
            }
        }

        @Nested
        @DisplayName("이미 존재하는 멱등키이며 상태가 TIMEOUT(타임아웃 구출 대상)인 경우")
        class Context_with_existing_timeout_key {

            @Test
            @DisplayName("스레드가 해당 락 선점에 성공(업데이트 로우수 1)하면 상태를 다시 PROCESSING으로 복구하고 재진입을 허용한다.")
            void it_recovers_timeout_key_to_processing_and_returns_it() {
                // given
                IdempotencyKey mockExistingKey = mock(IdempotencyKey.class);
                when(mockExistingKey.getStatus()).thenReturn(IdempotencyStatus.TIMEOUT);
                when(mockExistingKey.getId()).thenReturn(55L);

                when(idempotencyKeyRepository.findByMemberAndIdempotencyKey(mockMember, IDEMPOTENCY_KEY))
                        .thenReturn(Optional.of(mockExistingKey));

                // 벌크성 상태 선점 조건문 통과 명시 (1건 성공)
                when(idempotencyKeyRepository.updateStatusFromTimeoutToProcessing(55L))
                        .thenReturn(1);

                // when
                IdempotencyKey result = idempotencyManager.getOrCreateLock(mockMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY);

                // then
                assertThat(result).isEqualTo(mockExistingKey);
                // 엔티티 내부 갱신 도메인 메서드가 구동되었는지 검증
                verify(mockExistingKey, times(1)).startProcessingAgain(RAW_BODY);
                verify(idempotencyKeyRepository, never()).saveAndFlush(any());
            }

            @Test
            @DisplayName("동시 요청 중 다른 스레드가 찰나의 순간에 락을 먼저 선점(업데이트 로우수 0)했다면 ALREADY_PROCESSED_PAYMENT 예외를 발생시킨다.")
            void it_throws_already_processed_exception_when_another_thread_wins_timeout_lock() {
                // given
                IdempotencyKey mockExistingKey = mock(IdempotencyKey.class);
                when(mockExistingKey.getStatus()).thenReturn(IdempotencyStatus.TIMEOUT);
                when(mockExistingKey.getId()).thenReturn(55L);

                when(idempotencyKeyRepository.findByMemberAndIdempotencyKey(mockMember, IDEMPOTENCY_KEY))
                        .thenReturn(Optional.of(mockExistingKey));

                // 경쟁에서 밀린 경우 시뮬레이션 (0건 변경)
                when(idempotencyKeyRepository.updateStatusFromTimeoutToProcessing(55L))
                        .thenReturn(0);

                // when & then
                assertThatThrownBy(() -> idempotencyManager.getOrCreateLock(mockMember, IDEMPOTENCY_KEY, REQUEST_PATH, RAW_BODY))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_PROCESSED_PAYMENT);

                verify(mockExistingKey, never()).startProcessingAgain(anyString());
            }
        }
    }

    @Nested
    @DisplayName("상태 변경 최종 처리 메서드 군은")
    class Describe_UpdateStatusMethods {

        @Test
        @DisplayName("updateToSuccess 호출 시 엔티티 상태를 성공 정보로 변경하고 DB에 영구 보존한다.")
        void it_updates_idempotency_to_success() {
            // given
            IdempotencyKey mockKey = mock(IdempotencyKey.class);

            // when
            idempotencyManager.updateToSuccess(mockKey, 200, "{\"status\":\"DONE\"}");

            // then
            verify(mockKey, times(1)).updateSuccess(200, "{\"status\":\"DONE\"}");
            verify(idempotencyKeyRepository, times(1)).save(mockKey);
        }

        @Test
        @DisplayName("updateToFail 호출 시 엔티티 상태를 실패 정보로 변경하고 DB에 영구 보존한다.")
        void it_updates_idempotency_to_fail() {
            // given
            IdempotencyKey mockKey = mock(IdempotencyKey.class);

            // when
            idempotencyManager.updateToFail(mockKey, 400, "{\"error\":\"REJECTED\"}");

            // then
            verify(mockKey, times(1)).updateFailed(400, "{\"error\":\"REJECTED\"}");
            verify(idempotencyKeyRepository, times(1)).save(mockKey);
        }

        @Test
        @DisplayName("updateToFail 처리 중 예측하지 못한 DB 런타임 예외가 발생하더라도 비즈니스 전파 없이 안전하게 삼킨다(Swallow).")
        void it_swallows_exception_safely_when_update_to_fail_throws_exception() {
            // given
            IdempotencyKey mockKey = mock(IdempotencyKey.class);
            doThrow(new RuntimeException("DB Connection Dead")).when(idempotencyKeyRepository).save(mockKey);

            // when & then (예외가 밖으로 튀어나오지 않고 흡수되어 코드가 정상 종료되어야 함)
            idempotencyManager.updateToFail(mockKey, 500, "{\"error\":\"CRITICAL\"}");

            verify(mockKey, times(1)).updateFailed(500, "{\"error\":\"CRITICAL\"}");
            verify(idempotencyKeyRepository, times(1)).save(mockKey);
        }

        @Test
        @DisplayName("updateToTimeout 호출 시 엔티티 상태를 타임아웃 정보로 변경하고 DB에 영구 보존한다.")
        void it_updates_idempotency_to_timeout() {
            // given
            IdempotencyKey mockKey = mock(IdempotencyKey.class);

            // when
            idempotencyManager.updateToTimeout(mockKey, 408, "Gateway Timeout");

            // then
            verify(mockKey, times(1)).updateTimeout(408, "Gateway Timeout");
            verify(idempotencyKeyRepository, times(1)).save(mockKey);
        }
    }
}