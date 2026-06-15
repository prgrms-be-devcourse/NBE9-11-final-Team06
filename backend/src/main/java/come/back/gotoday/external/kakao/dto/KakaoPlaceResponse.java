package come.back.gotoday.external.kakao.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class KakaoPlaceResponse {

    private List<KakaoPlaceDocument> documents;
}