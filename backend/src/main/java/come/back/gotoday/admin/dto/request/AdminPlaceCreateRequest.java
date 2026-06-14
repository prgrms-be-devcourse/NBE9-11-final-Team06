package come.back.gotoday.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdminPlaceCreateRequest(

        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @NotBlank(message = "장소 이름은 필수입니다.")
        String name,

        @NotBlank(message = "주소는 필수입니다.")
        String address,

        String roadAddress,

        @NotNull(message = "위도는 필수입니다.")
        BigDecimal latitude,

        @NotNull(message = "경도는 필수입니다.")
        BigDecimal longitude,

        String phone,

        String placeUrl,

        String description,

        String externalId
) {
}