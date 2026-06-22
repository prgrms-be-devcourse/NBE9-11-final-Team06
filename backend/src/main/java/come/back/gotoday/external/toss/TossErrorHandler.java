package come.back.gotoday.external.toss;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class TossErrorHandler {

    private final TossResponseParser responseParser;

    public TossErrorHandler(TossResponseParser responseParser) {
        this.responseParser = responseParser;
    }

    public void handleTossError(RestClientResponseException e) {
        String errorBody = e.getResponseBodyAsString();
        String errorCode = responseParser.parseErrorCode(errorBody);
        HttpStatusCode status = e.getStatusCode();

        // 1. 4xx 클라이언트 에러 분기
        if (status.is4xxClientError()) {
            if (status.value() == 400) {
                throw new BusinessException(switch (errorCode) {
                    case "INVALID_CARD_NUMBER" -> ErrorCode.INVALID_CARD_NUMBER;
                    case "NOT_SUPPORTED_CARD_TYPE" -> ErrorCode.NOT_SUPPORTED_CARD_TYPE;
                    case "INVALID_CARD_PASSWORD" -> ErrorCode.INVALID_CARD_PASSWORD;
                    case "INVALID_CARD_EXPIRATION" -> ErrorCode.INVALID_CARD_EXPIRATION;
                    case "INVALID_CARD_IDENTITY" -> ErrorCode.INVALID_CARD_IDENTITY;
                    case "INVALID_REJECT_CARD" -> ErrorCode.INVALID_REJECT_CARD;
                    case "INVALID_STOPPED_CARD" -> ErrorCode.INVALID_STOPPED_CARD;
                    case "INVALID_BIRTH_DAY_FORMAT" -> ErrorCode.INVALID_BIRTH_DAY_FORMAT;
                    case "NOT_REGISTERED_CARD_COMPANY" -> ErrorCode.NOT_REGISTERED_CARD_COMPANY;
                    case "INVALID_EMAIL" -> ErrorCode.INVALID_EMAIL;
                    case "NOT_SUPPORTED_METHOD" -> ErrorCode.NOT_SUPPORTED_METHOD;
                    case "INVALID_REQUEST" -> ErrorCode.INVALID_REQUEST;
                    default -> ErrorCode.INVALID_REQUEST;
                });
            }
            if (status.value() == 403) {
                throw new BusinessException(switch (errorCode) {
                    case "EXCEED_MAX_AUTH_COUNT" -> ErrorCode.EXCEED_MAX_AUTH_COUNT;
                    case "REJECT_CARD_COMPANY" -> ErrorCode.REJECT_CARD_COMPANY;
                    case "REJECT_ACCOUNT_PAYMENT" -> ErrorCode.REJECT_ACCOUNT_PAYMENT;
                    default -> ErrorCode.FORBIDDEN_REQUEST;
                });
            }
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // 2. 5xx 서버 에러 분기
        if (status.is5xxServerError()) {
            throw new BusinessException(switch (errorCode) {
                case "COMMON_ERROR" -> ErrorCode.COMMON_ERROR;
                default -> ErrorCode.INTERNAL_SERVER_ERROR;
            });
        }

        // 4xx, 5xx 이외의 예기치 못한 에러 상태 코드 처리
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}