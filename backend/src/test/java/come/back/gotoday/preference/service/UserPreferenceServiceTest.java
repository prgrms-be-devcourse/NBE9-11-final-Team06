package come.back.gotoday.preference.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.category.type.CategoryType;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.preference.dto.UserPreferenceCreateRequest;
import come.back.gotoday.preference.dto.UserPreferenceResponse;
import come.back.gotoday.preference.dto.UserPreferenceUpdateRequest;
import come.back.gotoday.preference.entity.CompanionType;
import come.back.gotoday.preference.entity.MobilityLevel;
import come.back.gotoday.preference.entity.UserPreference;
import come.back.gotoday.preference.entity.UserPreferenceCategory;
import come.back.gotoday.preference.repository.UserPreferenceCategoryRepository;
import come.back.gotoday.preference.repository.UserPreferenceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private UserPreferenceCategoryRepository userPreferenceCategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private UserPreferenceService userPreferenceService;

    @Test
    @DisplayName("선호 정보를 등록한다")
    void createPreference_success() {
        // given
        Long memberId = 1L;

        Member member = createMember(memberId);
        Category category1 = createCategory(1L, "무용", CategoryType.EVENT);
        Category category2 = createCategory(2L, "축제-기타", CategoryType.EVENT);

        UserPreferenceCreateRequest request = new UserPreferenceCreateRequest(
                "홍대",
                List.of(1L, 2L),
                CompanionType.FRIEND,
                MobilityLevel.NORMAL,
                true
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userPreferenceRepository.existsByMemberId(memberId)).thenReturn(false);
        when(categoryRepository.findByIdIn(anyCollection())).thenReturn(List.of(category1, category2));
        when(userPreferenceRepository.save(any(UserPreference.class)))
                .thenAnswer(invocation -> {
                    UserPreference userPreference = invocation.getArgument(0);
                    ReflectionTestUtils.setField(userPreference, "id", 10L);
                    return userPreference;
                });

        // when
        UserPreferenceResponse response = userPreferenceService.createPreference(memberId, request);

        // then
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.preferredArea()).isEqualTo("홍대");
        assertThat(response.companionType()).isEqualTo(CompanionType.FRIEND);
        assertThat(response.mobilityLevel()).isEqualTo(MobilityLevel.NORMAL);
        assertThat(response.avoidCrowded()).isTrue();
        assertThat(response.categories()).hasSize(2);
        assertThat(response.categories())
                .extracting("id")
                .containsExactly(1L, 2L);

        ArgumentCaptor<UserPreference> preferenceCaptor =
                ArgumentCaptor.forClass(UserPreference.class);

        verify(userPreferenceRepository).save(preferenceCaptor.capture());

        UserPreference savedPreference = preferenceCaptor.getValue();

        assertThat(savedPreference.getMember()).isEqualTo(member);
        assertThat(savedPreference.getPreferredArea()).isEqualTo("홍대");
        assertThat(savedPreference.getCompanionType()).isEqualTo(CompanionType.FRIEND);
        assertThat(savedPreference.getMobilityLevel()).isEqualTo(MobilityLevel.NORMAL);
        assertThat(savedPreference.getAvoidCrowded()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserPreferenceCategory>> preferenceCategoriesCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(userPreferenceCategoryRepository).saveAll(preferenceCategoriesCaptor.capture());

        List<UserPreferenceCategory> savedPreferenceCategories =
                preferenceCategoriesCaptor.getValue();

        assertThat(savedPreferenceCategories).hasSize(2);
        assertThat(savedPreferenceCategories)
                .extracting(UserPreferenceCategory::getCategory)
                .containsExactly(category1, category2);
    }

    @Test
    @DisplayName("선호 정보 등록 시 중복 선호 정보가 있으면 예외가 발생한다")
    void createPreference_throwException_whenDuplicatePreference() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId);

        UserPreferenceCreateRequest request = new UserPreferenceCreateRequest(
                "홍대",
                List.of(1L),
                CompanionType.FRIEND,
                MobilityLevel.NORMAL,
                true
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userPreferenceRepository.existsByMemberId(memberId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userPreferenceService.createPreference(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.DUPLICATE_PREFERENCE)
                );

        verifyNoInteractions(categoryRepository);
        verify(userPreferenceRepository, never()).save(any());
        verify(userPreferenceCategoryRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("선호 정보 등록 시 회원이 없으면 예외가 발생한다")
    void createPreference_throwException_whenMemberNotFound() {
        // given
        Long memberId = 1L;

        UserPreferenceCreateRequest request = new UserPreferenceCreateRequest(
                "홍대",
                List.of(1L),
                CompanionType.FRIEND,
                MobilityLevel.NORMAL,
                true
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userPreferenceService.createPreference(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND)
                );

        verifyNoInteractions(categoryRepository);
        verify(userPreferenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("선호 정보 등록 시 존재하지 않는 카테고리가 있으면 예외가 발생한다")
    void createPreference_throwException_whenCategoryNotFound() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId);
        Category category = createCategory(1L, "무용", CategoryType.EVENT);

        UserPreferenceCreateRequest request = new UserPreferenceCreateRequest(
                "홍대",
                List.of(1L, 999L),
                CompanionType.FRIEND,
                MobilityLevel.NORMAL,
                true
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userPreferenceRepository.existsByMemberId(memberId)).thenReturn(false);
        when(categoryRepository.findByIdIn(anyCollection())).thenReturn(List.of(category));

        // when & then
        assertThatThrownBy(() -> userPreferenceService.createPreference(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND)
                );

        verify(userPreferenceRepository, never()).save(any());
        verify(userPreferenceCategoryRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("내 선호 정보를 조회한다")
    void getMyPreference_success() {
        // given
        Long memberId = 1L;
        Long preferenceId = 10L;

        Member member = createMember(memberId);
        UserPreference userPreference = createUserPreference(
                preferenceId,
                member,
                "성수",
                CompanionType.COUPLE,
                MobilityLevel.LOW,
                false
        );

        Category category1 = createCategory(1L, "무용", CategoryType.EVENT);
        Category category2 = createCategory(2L, "축제-기타", CategoryType.EVENT);

        UserPreferenceCategory preferenceCategory1 =
                UserPreferenceCategory.create(userPreference, category1);
        UserPreferenceCategory preferenceCategory2 =
                UserPreferenceCategory.create(userPreference, category2);

        when(userPreferenceRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(userPreference));
        when(userPreferenceCategoryRepository.findByUserPreferenceIdWithCategory(preferenceId))
                .thenReturn(List.of(preferenceCategory1, preferenceCategory2));

        // when
        UserPreferenceResponse response = userPreferenceService.getMyPreference(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(preferenceId);
        assertThat(response.preferredArea()).isEqualTo("성수");
        assertThat(response.companionType()).isEqualTo(CompanionType.COUPLE);
        assertThat(response.mobilityLevel()).isEqualTo(MobilityLevel.LOW);
        assertThat(response.avoidCrowded()).isFalse();
        assertThat(response.categories()).hasSize(2);
        assertThat(response.categories())
                .extracting("name")
                .containsExactly("무용", "축제-기타");
    }

    @Test
    @DisplayName("내 선호 정보가 없으면 null을 반환한다")
    void getMyPreference_returnNull_whenNotExists() {
        // given
        Long memberId = 1L;

        when(userPreferenceRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

        // when
        UserPreferenceResponse response = userPreferenceService.getMyPreference(memberId);

        // then
        assertThat(response).isNull();

        verifyNoInteractions(userPreferenceCategoryRepository);
    }

    @Test
    @DisplayName("선호 정보를 수정한다")
    void updatePreference_success() {
        // given
        Long memberId = 1L;
        Long preferenceId = 10L;

        Member member = createMember(memberId);
        UserPreference userPreference = createUserPreference(
                preferenceId,
                member,
                "홍대",
                CompanionType.FRIEND,
                MobilityLevel.NORMAL,
                true
        );

        Category category = createCategory(1L, "무용", CategoryType.EVENT);
        UserPreferenceCategory preferenceCategory =
                UserPreferenceCategory.create(userPreference, category);

        UserPreferenceUpdateRequest request = new UserPreferenceUpdateRequest(
                "성수",
                null,
                CompanionType.COUPLE,
                MobilityLevel.LOW,
                false
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userPreferenceRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(userPreference));
        when(userPreferenceCategoryRepository.findByUserPreferenceIdWithCategory(preferenceId))
                .thenReturn(List.of(preferenceCategory));

        // when
        UserPreferenceResponse response = userPreferenceService.updatePreference(memberId, request);

        // then
        assertThat(response.id()).isEqualTo(preferenceId);
        assertThat(response.preferredArea()).isEqualTo("성수");
        assertThat(response.companionType()).isEqualTo(CompanionType.COUPLE);
        assertThat(response.mobilityLevel()).isEqualTo(MobilityLevel.LOW);
        assertThat(response.avoidCrowded()).isFalse();
        assertThat(response.categories()).hasSize(1);

        assertThat(userPreference.getPreferredArea()).isEqualTo("성수");
        assertThat(userPreference.getCompanionType()).isEqualTo(CompanionType.COUPLE);
        assertThat(userPreference.getMobilityLevel()).isEqualTo(MobilityLevel.LOW);
        assertThat(userPreference.getAvoidCrowded()).isFalse();

        verify(userPreferenceCategoryRepository, never()).deleteByUserPreferenceId(any());
        verify(userPreferenceCategoryRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("선호 정보 수정 시 카테고리 목록이 요청되면 기존 연결을 삭제하고 새로 저장한다")
    void updatePreference_updateCategories_whenCategoryIdsRequested() {
        // given
        Long memberId = 1L;
        Long preferenceId = 10L;

        Member member = createMember(memberId);
        UserPreference userPreference = createUserPreference(
                preferenceId,
                member,
                "홍대",
                CompanionType.FRIEND,
                MobilityLevel.NORMAL,
                true
        );

        Category category1 = createCategory(1L, "무용", CategoryType.EVENT);
        Category category2 = createCategory(2L, "축제-기타", CategoryType.EVENT);

        UserPreferenceUpdateRequest request = new UserPreferenceUpdateRequest(
                null,
                List.of(1L, 2L),
                null,
                null,
                null
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userPreferenceRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(userPreference));
        when(categoryRepository.findByIdIn(anyCollection()))
                .thenReturn(List.of(category1, category2));

        // when
        UserPreferenceResponse response = userPreferenceService.updatePreference(memberId, request);

        // then
        assertThat(response.id()).isEqualTo(preferenceId);
        assertThat(response.preferredArea()).isEqualTo("홍대");
        assertThat(response.categories()).hasSize(2);
        assertThat(response.categories())
                .extracting("id")
                .containsExactly(1L, 2L);

        verify(userPreferenceCategoryRepository).deleteByUserPreferenceId(preferenceId);
        verify(userPreferenceCategoryRepository).saveAll(any());
    }

    @Test
    @DisplayName("선호 정보 수정 요청에 변경할 필드가 없으면 예외가 발생한다")
    void updatePreference_throwException_whenRequestHasNoFields() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId);

        UserPreferenceUpdateRequest request = new UserPreferenceUpdateRequest(
                null,
                null,
                null,
                null,
                null
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> userPreferenceService.updatePreference(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REQUEST)
                );

        verify(userPreferenceRepository, never()).findByMemberId(memberId);
    }

    @Test
    @DisplayName("선호 정보 수정 시 선호 정보가 없으면 예외가 발생한다")
    void updatePreference_throwException_whenPreferenceNotFound() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId);

        UserPreferenceUpdateRequest request = new UserPreferenceUpdateRequest(
                "성수",
                null,
                null,
                null,
                null
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userPreferenceRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userPreferenceService.updatePreference(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.PREFERENCE_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("선호 정보를 삭제한다")
    void deletePreference_success() {
        // given
        Long memberId = 1L;
        Long preferenceId = 10L;

        Member member = createMember(memberId);
        UserPreference userPreference = createUserPreference(
                preferenceId,
                member,
                "홍대",
                CompanionType.FRIEND,
                MobilityLevel.NORMAL,
                true
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userPreferenceRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(userPreference));

        // when
        userPreferenceService.deletePreference(memberId);

        // then
        verify(userPreferenceCategoryRepository).deleteByUserPreferenceId(preferenceId);
        verify(userPreferenceRepository).delete(userPreference);
    }

    @Test
    @DisplayName("선호 정보 삭제 시 선호 정보가 없으면 예외가 발생한다")
    void deletePreference_throwException_whenPreferenceNotFound() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userPreferenceRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userPreferenceService.deletePreference(memberId))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.PREFERENCE_NOT_FOUND)
                );

        verify(userPreferenceCategoryRepository, never()).deleteByUserPreferenceId(any());
        verify(userPreferenceRepository, never()).delete(any());
    }

    @Test
    @DisplayName("카테고리 id 중복 요청 시 중복을 제거하고 조회한다")
    void createPreference_distinctCategoryIds() {
        // given
        Long memberId = 1L;

        Member member = createMember(memberId);
        Category category1 = createCategory(1L, "무용", CategoryType.EVENT);
        Category category2 = createCategory(2L, "축제-기타", CategoryType.EVENT);

        UserPreferenceCreateRequest request = new UserPreferenceCreateRequest(
                "홍대",
                List.of(1L, 1L, 2L),
                CompanionType.FRIEND,
                MobilityLevel.NORMAL,
                true
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userPreferenceRepository.existsByMemberId(memberId)).thenReturn(false);
        when(categoryRepository.findByIdIn(anyCollection())).thenReturn(List.of(category1, category2));
        when(userPreferenceRepository.save(any(UserPreference.class)))
                .thenAnswer(invocation -> {
                    UserPreference userPreference = invocation.getArgument(0);
                    ReflectionTestUtils.setField(userPreference, "id", 10L);
                    return userPreference;
                });

        // when
        userPreferenceService.createPreference(memberId, request);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> categoryIdsCaptor =
                ArgumentCaptor.forClass(Collection.class);

        verify(categoryRepository).findByIdIn(categoryIdsCaptor.capture());

        assertThat(categoryIdsCaptor.getValue())
                .containsExactly(1L, 2L);
    }

    private Member createMember(Long memberId) {
        Member member = Member.create(
                "test@example.com",
                "encoded-password",
                "테스터",
                "USER",
                "ACTIVE"
        );

        ReflectionTestUtils.setField(member, "id", memberId);

        return member;
    }

    private Category createCategory(Long categoryId, String name, CategoryType type) {
        Category category = Category.create(name, type);

        ReflectionTestUtils.setField(category, "id", categoryId);

        return category;
    }

    private UserPreference createUserPreference(
            Long preferenceId,
            Member member,
            String preferredArea,
            CompanionType companionType,
            MobilityLevel mobilityLevel,
            Boolean avoidCrowded
    ) {
        UserPreference userPreference = UserPreference.create(
                member,
                preferredArea,
                companionType,
                mobilityLevel,
                avoidCrowded
        );

        ReflectionTestUtils.setField(userPreference, "id", preferenceId);

        return userPreference;
    }
}