package come.back.gotoday.payment.idempotency.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import come.back.gotoday.payment.idempotency.enums.IdempotencyStatus;
import come.back.gotoday.payment.idempotency.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdempotencyManager {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    /**
     * 외부 API 호출 전, 독자적인 트랜잭션을 열어 PROCESSING 상태를 즉시 DB에 커밋합니다.
     * 이를 통해 찰나의 순간에 들어오는 중복 요청을 DB 유니크 제약조건으로 완전히 차단합니다.
     */
    @Transactional
    public IdempotencyKey getOrCreateLock(Member member, String idempotencyKey, String requestPath, String rawBody) {

        // 1. [추가] 이미 유입된 적이 있는 클라이언트 생성 멱등키인지 사전 식별
        Optional<IdempotencyKey> existingKeyOpt = idempotencyKeyRepository.findByMemberAndIdempotencyKey(member, idempotencyKey);

        if (existingKeyOpt.isPresent()) {
            IdempotencyKey existingKey = existingKeyOpt.get();

            // Case A: 이미 완벽하게 성공했던 결제 건 -> 패사드에서 Early Return할 수 있도록 그대로 엔티티 반환
            if (existingKey.getStatus() == IdempotencyStatus.SUCCESS) {
                return existingKey;
            }

            // Case B: 현재 찰나의 순간에 처리 중인 상태 -> 더블 클릭 연타 차단 예외 발생
            if (existingKey.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
            }

            // Case C: 이미 완전히 실패(FAILED) 처리로 종결된 키가 재요청된 경우 ->
            // 프론트엔드 설계상 실패 시 무조건 '새로운 UUID 키'를 발행해야 하므로 동일 키 재진입은 비정상 차단 처리
            if (existingKey.getStatus() == IdempotencyStatus.FAILED) {
                throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
            }
        }

        try {
            // 엔티티 정의에 맞게 4개의 인자(member, key, path, body)를 정확히 전달
            IdempotencyKey keyEntity = IdempotencyKey.startProcessing(member, idempotencyKey, requestPath, rawBody);
            return idempotencyKeyRepository.saveAndFlush(keyEntity);
        } catch (DataIntegrityViolationException e) {
//            log.warn("중복 결제 요청 진입 차단 - memberId: {}, idempotencyKey: {}", member.getId(), idempotencyKey);
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
        }
    }

    /**
     * 최종 성공 시 호출되어 결과를 저장합니다. (역시 즉시 커밋)
     */
    @Transactional
    public void updateToSuccess(IdempotencyKey idempotencyKey, int responseCode, String responseBody) {
        idempotencyKey.updateSuccess(responseCode, responseBody);
        idempotencyKeyRepository.save(idempotencyKey);
    }

    @Transactional
    public void updateToFail(IdempotencyKey idempotencyKey, int responseCode, String responseBody) {
        try {
            // 엔티티 실물 메서드인 updateFailed 호출
            idempotencyKey.updateFailed(responseCode, responseBody);
            idempotencyKeyRepository.save(idempotencyKey);
        } catch (Exception e) {
            // 요청하신 대로 에러 로그를 비워두거나 swallow 처리
        }
    }
}