package come.back.gotoday.preference.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.preference.dto.UserPreferenceCreateRequest;
import come.back.gotoday.preference.dto.UserPreferenceResponse;
import come.back.gotoday.preference.dto.UserPreferenceUpdateRequest;
import come.back.gotoday.preference.entity.UserPreference;
import come.back.gotoday.preference.entity.UserPreferenceCategory;
import come.back.gotoday.preference.repository.UserPreferenceCategoryRepository;
import come.back.gotoday.preference.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserPreferenceCategoryRepository userPreferenceCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public UserPreferenceResponse createPreference(
            Long memberId,
            UserPreferenceCreateRequest request
    ) {
        Member member = findActiveMember(memberId);

        validateDuplicatePreference(memberId);

        List<Category> categories = findCategories(request.categoryIds());

        UserPreference userPreference = UserPreference.create(
                member,
                request.preferredArea(),
                request.companionType(),
                request.mobilityLevel(),
                request.avoidCrowded()
        );

        UserPreference savedPreference = userPreferenceRepository.save(userPreference);

        savePreferenceCategories(savedPreference, categories);

        return UserPreferenceResponse.of(savedPreference, categories);
    }

    @Transactional(readOnly = true)
    public UserPreferenceResponse getMyPreference(Long memberId) {
        UserPreference userPreference = userPreferenceRepository.findByMemberId(memberId)
                .orElse(null);

        if (userPreference == null) {
            return null;
        }

        List<Category> categories = findCategoriesByPreference(userPreference.getId());

        return UserPreferenceResponse.of(userPreference, categories);
    }

    @Transactional
    public UserPreferenceResponse updatePreference(
            Long memberId,
            UserPreferenceUpdateRequest request
    ) {
        findActiveMember(memberId);

        UserPreference userPreference = findPreferenceByMemberId(memberId);
        List<Category> categories = findCategories(request.categoryIds());

        userPreference.update(
                request.preferredArea(),
                request.companionType(),
                request.mobilityLevel(),
                request.avoidCrowded()
        );

        userPreferenceCategoryRepository.deleteByUserPreferenceId(userPreference.getId());
        savePreferenceCategories(userPreference, categories);

        return UserPreferenceResponse.of(userPreference, categories);
    }

    @Transactional
    public void deletePreference(Long memberId) {
        findActiveMember(memberId);

        UserPreference userPreference = findPreferenceByMemberId(memberId);

        userPreferenceCategoryRepository.deleteByUserPreferenceId(userPreference.getId());
        userPreferenceRepository.delete(userPreference);
    }

    private Member findActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.isDeleted()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return member;
    }

    private UserPreference findPreferenceByMemberId(Long memberId) {
        return userPreferenceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));
    }

    private void validateDuplicatePreference(Long memberId) {
        if (userPreferenceRepository.existsByMemberId(memberId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PREFERENCE);
        }
    }

    private List<Category> findCategories(List<Long> categoryIds) {
        List<Long> distinctCategoryIds = categoryIds.stream()
                .distinct()
                .toList();

        List<Category> categories = categoryRepository.findByIdIn(distinctCategoryIds);

        if (categories.size() != distinctCategoryIds.size()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        return categories;
    }

    private List<Category> findCategoriesByPreference(Long userPreferenceId) {
        return userPreferenceCategoryRepository.findByUserPreferenceId(userPreferenceId)
                .stream()
                .map(UserPreferenceCategory::getCategory)
                .toList();
    }

    private void savePreferenceCategories(
            UserPreference userPreference,
            List<Category> categories
    ) {
        List<UserPreferenceCategory> preferenceCategories = categories.stream()
                .map(category -> UserPreferenceCategory.create(userPreference, category))
                .toList();

        userPreferenceCategoryRepository.saveAll(preferenceCategories);
    }
}