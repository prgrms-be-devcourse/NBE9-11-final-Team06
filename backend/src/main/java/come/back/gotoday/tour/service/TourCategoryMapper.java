package come.back.gotoday.tour.service;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TourCategoryMapper {

    public Optional<String> mapToCategoryName(String cat1, String cat2, String cat3) {
        return Optional.of("관광지");
    }

    public String mapDetailCategoryName(String cat1, String cat2, String cat3) {
        if (cat3 == null || cat3.isBlank()) {
            return "기타 관광지";
        }

        return switch (cat3) {
            // 자연관광지
            case "A01010100", "A01010200", "A01010400", "A01010500",
                    "A01010700", "A01010900", "A01011800" -> "자연관광지";

            // 역사관광지
            case "A02010100", "A02010200", "A02010300", "A02010400",
                    "A02010500", "A02010600", "A02010700", "A02010800",
                    "A02010900", "A02011000" -> "역사관광지";

            // 휴양관광지
            case "A02020200", "A02020300", "A02020400", "A02020500",
                    "A02020600", "A02020700", "A02020800" -> "휴양관광지";

            // 체험관광지
            case "A02030100", "A02030200", "A02030400", "A02030600" -> "체험관광지";

            // 산업관광지
            case "A02040600", "A02040800", "A02040900", "A02041000" -> "산업관광지";

            // 건축/조형물
            case "A02050100", "A02050200", "A02050400", "A02050600" -> "건축/조형물";

            default -> "기타 관광지";
        };
    }
}