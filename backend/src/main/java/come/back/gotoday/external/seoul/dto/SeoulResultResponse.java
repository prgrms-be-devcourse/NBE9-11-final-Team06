package come.back.gotoday.external.seoul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SeoulResultResponse(
        @JsonProperty("RESULT") Result result
) {
    public record Result(
            @JsonProperty("CODE") String code,
            @JsonProperty("MESSAGE") String message
    ) {}

    // 에러인지 확인하는 편의 메서드 (INFO-000이 아니면 모두 에러 또는 데이터 없음)
    public boolean isError() {
        if (result == null || result.code() == null) return false;
        // INFO-000(정상), INFO-200(데이터 없음 - 상황에 따라 정상 혹은 예외 처리)
        return !result.code().equals("INFO-000") && !result.code().equals("INFO-200");
    }

    // 데이터 자체가 없는 상황인지 확인하는 메서드
    public boolean isNoData() {
        return result != null && "INFO-200".equals(result.code());
    }
}