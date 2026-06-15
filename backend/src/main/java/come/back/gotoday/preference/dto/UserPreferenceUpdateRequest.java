package come.back.gotoday.preference.dto;

import come.back.gotoday.preference.entity.CompanionType;
import come.back.gotoday.preference.entity.MobilityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UserPreferenceUpdateRequest(

        @NotBlank(message = "선호 지역은 필수입니다.")
        String preferredArea,

        @NotEmpty(message = "선호 카테고리는 1개 이상 선택해야 합니다.")
        List<Long> categoryIds,

        @NotNull(message = "동행 유형은 필수입니다.")
        CompanionType companionType,

        @NotNull(message = "이동 강도는 필수입니다.")
        MobilityLevel mobilityLevel,

        @NotNull(message = "혼잡도 선호 여부는 필수입니다.")
        Boolean avoidCrowded
) {
}