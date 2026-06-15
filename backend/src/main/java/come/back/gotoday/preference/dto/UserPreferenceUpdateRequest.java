package come.back.gotoday.preference.dto;

import come.back.gotoday.preference.entity.CompanionType;
import come.back.gotoday.preference.entity.MobilityLevel;

import java.util.List;

public record UserPreferenceUpdateRequest(
        String preferredArea,
        List<Long> categoryIds,
        CompanionType companionType,
        MobilityLevel mobilityLevel,
        Boolean avoidCrowded
) {
}