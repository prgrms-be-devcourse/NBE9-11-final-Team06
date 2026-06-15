package come.back.gotoday.preference.dto;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.preference.entity.CompanionType;
import come.back.gotoday.preference.entity.MobilityLevel;
import come.back.gotoday.preference.entity.UserPreference;

import java.util.List;

public record UserPreferenceResponse(
        Long id,
        String preferredArea,
        List<PreferenceCategoryResponse> categories,
        CompanionType companionType,
        MobilityLevel mobilityLevel,
        Boolean avoidCrowded
) {

    public static UserPreferenceResponse of(
            UserPreference userPreference,
            List<Category> categories
    ) {
        return new UserPreferenceResponse(
                userPreference.getId(),
                userPreference.getPreferredArea(),
                categories.stream()
                        .map(PreferenceCategoryResponse::from)
                        .toList(),
                userPreference.getCompanionType(),
                userPreference.getMobilityLevel(),
                userPreference.getAvoidCrowded()
        );
    }
}