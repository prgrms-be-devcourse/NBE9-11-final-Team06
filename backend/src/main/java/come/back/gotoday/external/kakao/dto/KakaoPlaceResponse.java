package come.back.gotoday.external.kakao.dto;

import java.util.List;

public record KakaoPlaceResponse(
        List<KakaoPlaceDocument> documents
) {
}