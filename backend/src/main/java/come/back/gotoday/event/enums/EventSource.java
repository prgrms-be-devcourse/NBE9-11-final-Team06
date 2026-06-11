package come.back.gotoday.event.enums;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventSource {
    //어디 api에서 가져왔는지 자유롭게 추가 가능
    SEOUL_API("SEOUL_API", "서울시 공공 API"),
    MANUAL("MANUAL", "관리자 수동 등록");

    private final String code;
    private final String description;
}