package come.back.gotoday.recommend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RecommendationCourseCreateRequest(
        @Size(max = 50, message = "코스 제목은 50자 이하여야 합니다.")
        String title,

        @NotNull(message = "시작일은 필수입니다.")
        @FutureOrPresent(message = "시작일은 오늘 이후여야 합니다.")
        LocalDate startDate,

        @NotNull(message = "종료일은 필수입니다.")
        @FutureOrPresent(message = "종료일은 오늘 이후여야 합니다.")
        LocalDate endDate,

        @Min(value = 1, message = "추천 개수는 1개 이상이어야 합니다.")
        @Max(value = 10, message = "추천 개수는 10개 이하여야 합니다.")
        Integer topK,

        @Size(max = 30, message = "지역명은 30자 이하여야 합니다.")
        String area,

        @Size(max = 5, message = "카테고리는 최대 5개까지 선택할 수 있습니다.")
        List<String> categories,

        @Size(max = 30, message = "동행 유형은 30자 이하여야 합니다.")
        String companionType,

        @Size(max = 100, message = "출발지 주소는 100자 이하여야 합니다.")
        String address,

        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        Double longitude
) {
    @AssertTrue(message = "종료일은 시작일보다 빠를 수 없습니다.")
    public boolean isValidPeriod() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }

    public int getTopKOrDefault() {
        return topK == null ? 3 : topK;
    }

    public String getTitleOrDefault() {
        return title == null || title.isBlank() ? "추천 코스" : title;
    }

    @AssertTrue(message = "위도와 경도는 함께 입력해야 합니다.")
    public boolean isValidCoordinates() {
        return (latitude == null && longitude == null)
                || (latitude != null && longitude != null);
    }
}