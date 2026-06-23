package come.back.gotoday.external.toss.enums;

import java.util.Arrays;

/**
 * 토스페이먼츠 공식 카드사 코드 규격 매핑 Enum
 */
public enum TossCardCode {
    SHINHAN("41", "신한카드"),
    HYUNDAI("61", "현대카드"),
    SAMSUNG("51", "삼성카드"),
    KB("11", "국민카드"),
    LOTTE("71", "롯데카드"),
    NH("91", "농협카드"),
    HANA("21", "하나카드"),
    BC("31", "BC카드"),
    WOORI("4W", "우리카드"),
    CITI("36", "씨티카드"),
    KAKAOBANK("15", "카카오뱅크"),
    TOSS("24", "토스뱅크"),
    KABBANK("32", "부산은행"),
    KJBANK("33", "광주은행"),
    JEJUBANK("34", "제주은행"),
    JBANK("35", "전북은행"),
    SUHYUP("48", "수협은행"),
    POST("12", "우체국"),
    SAVINGBANK("39", "저축은행"),
    SAEMAUL("38", "새마을금고"),
    SHINHYUP("42", "신협"),
    UNKNOWN("00", "기타카드");

    private final String code;
    private final String koreanName;

    TossCardCode(String code, String koreanName) {
        this.code = code;
        this.koreanName = koreanName;
    }

    public String getCode() {
        return code;
    }

    public String getKoreanName() {
        return koreanName;
    }

    /**
     * 토스 issuerCode를 입력받아 정확한 한글 카드사명을 반환
     */
    public static String getCardNameByCode(String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN.getKoreanName();
        }

        return Arrays.stream(TossCardCode.values())
                .filter(c -> c.getCode().equals(code.trim()))
                .findFirst()
                .map(TossCardCode::getKoreanName)
                .orElse("카드사(코드: " + code + ")"); // 혹시 모를 신규 코드 대응
    }
}