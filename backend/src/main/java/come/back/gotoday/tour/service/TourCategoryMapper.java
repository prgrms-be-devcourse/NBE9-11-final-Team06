package come.back.gotoday.tour.service;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TourCategoryMapper {

    public Optional<String> mapToCategoryName(String cat1, String cat2, String cat3) {
        return Optional.of("관광지");
    }
}