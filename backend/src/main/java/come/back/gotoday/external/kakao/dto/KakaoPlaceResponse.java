package come.back.gotoday.external.kakao.dto;

import java.util.List;

public record KakaoPlaceResponse(
        List<KakaoPlaceDocument> documents
) {
    public static KakaoPlaceResponse empty() {
        return new KakaoPlaceResponse(List.of());
    }
}