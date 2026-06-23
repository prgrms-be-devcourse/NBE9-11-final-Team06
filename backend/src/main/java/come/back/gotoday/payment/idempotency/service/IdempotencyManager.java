package come.back.gotoday.payment.idempotency.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.payment.idempotency.entity.IdempotencyKey;
import come.back.gotoday.payment.idempotency.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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