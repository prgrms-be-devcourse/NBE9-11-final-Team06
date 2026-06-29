package come.back.gotoday.preference.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.preference.dto.UserPreferenceCreateRequest;
import come.back.gotoday.preference.dto.UserPreferenceResponse;
import come.back.gotoday.preference.dto.UserPreferenceUpdateRequest;
import come.back.gotoday.preference.entity.CompanionType;
import come.back.gotoday.preference.entity.MobilityLevel;
import come.back.gotoday.preference.service.UserPreferenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceControllerTest {

    @Mock
    private UserPreferenceService userPreferenceService;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private UserPreferenceController userPreferenceController;

    @Test
    @DisplayName("내 선호 정보 등록에 성공하면 201 Created와 선호 정보를 응답한다")
    void createMyPreference_success() {
        // given
        Long memberId = 1L;

        UserPreferenceCreateRequest request = new UserPreferenceCreateRequest(
                "홍대",
                List.of(1L, 2L),
                CompanionType.FRIEND,
                MobilityLevel.NORMAL,
                true
        );

        UserPreferenceResponse preferenceResponse = createPreferenceResponse(
                10L,
                "홍대",
                CompanionType.FRIEND,
                MobilityLevel.NORMAL,
                true
        );

        when(userDetails.getMemberId()).thenReturn(memberId);
        when(userPreferenceService.createPreference(memberId, request))
                .thenReturn(preferenceResponse);

        // when
        ResponseEntity<ApiResponse<UserPreferenceResponse>> response =
                userPreferenceController.createMyPreference(userDetails, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(201);

        ApiResponse<UserPreferenceResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("선호 정보 등록에 성공했습니다.");
        assertThat(ReflectionTestUtils.getField(body, "data"))
                .isEqualTo(preferenceResponse);

        verify(userDetails).getMemberId();
        verify(userPreferenceService).createPreference(memberId, request);
    }

    @Test
    @DisplayName("내 선호 정보 조회에 성공하면 선호 정보를 응답한다")
    void getMyPreference_success() {
        // given
        Long memberId = 1L;

        UserPreferenceResponse preferenceResponse = createPreferenceResponse(
                10L,
                "성수",
                CompanionType.COUPLE,
                MobilityLevel.LOW,
                false
        );

        when(userDetails.getMemberId()).thenReturn(memberId);
        when(userPreferenceService.getMyPreference(memberId))
                .thenReturn(preferenceResponse);

        // when
        ResponseEntity<ApiResponse<UserPreferenceResponse>> response =
                userPreferenceController.getMyPreference(userDetails);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ApiResponse<UserPreferenceResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("선호 정보 조회에 성공했습니다.");
        assertThat(ReflectionTestUtils.getField(body, "data"))
                .isEqualTo(preferenceResponse);

        verify(userDetails).getMemberId();
        verify(userPreferenceService).getMyPreference(memberId);
    }

    @Test
    @DisplayName("등록된 선호 정보가 없으면 data는 null로 응답한다")
    void getMyPreference_returnNull_whenNotExists() {
        // given
        Long memberId = 1L;

        when(userDetails.getMemberId()).thenReturn(memberId);
        when(userPreferenceService.getMyPreference(memberId))
                .thenReturn(null);

        // when
        ResponseEntity<ApiResponse<UserPreferenceResponse>> response =
                userPreferenceController.getMyPreference(userDetails);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ApiResponse<UserPreferenceResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("등록된 선호 정보가 없습니다.");
        assertThat(ReflectionTestUtils.getField(body, "data"))
                .isNull();

        verify(userDetails).getMemberId();
        verify(userPreferenceService).getMyPreference(memberId);
    }

    @Test
    @DisplayName("내 선호 정보 수정에 성공하면 수정된 선호 정보를 응답한다")
    void updateMyPreference_success() {
        // given
        Long memberId = 1L;

        UserPreferenceUpdateRequest request = new UserPreferenceUpdateRequest(
                "성수",
                List.of(1L, 2L),
                CompanionType.COUPLE,
                MobilityLevel.LOW,
                false
        );

        UserPreferenceResponse preferenceResponse = createPreferenceResponse(
                10L,
                "성수",
                CompanionType.COUPLE,
                MobilityLevel.LOW,
                false
        );

        when(userDetails.getMemberId()).thenReturn(memberId);
        when(userPreferenceService.updatePreference(memberId, request))
                .thenReturn(preferenceResponse);

        // when
        ResponseEntity<ApiResponse<UserPreferenceResponse>> response =
                userPreferenceController.updateMyPreference(userDetails, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ApiResponse<UserPreferenceResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("선호 정보 수정에 성공했습니다.");
        assertThat(ReflectionTestUtils.getField(body, "data"))
                .isEqualTo(preferenceResponse);

        verify(userDetails).getMemberId();
        verify(userPreferenceService).updatePreference(memberId, request);
    }

    @Test
    @DisplayName("내 선호 정보 삭제에 성공하면 성공 응답을 반환한다")
    void deleteMyPreference_success() {
        // given
        Long memberId = 1L;

        when(userDetails.getMemberId()).thenReturn(memberId);
        doNothing().when(userPreferenceService).deletePreference(memberId);

        // when
        ResponseEntity<ApiResponse<Void>> response =
                userPreferenceController.deleteMyPreference(userDetails);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("선호 정보 삭제에 성공했습니다.");

        verify(userDetails).getMemberId();
        verify(userPreferenceService).deletePreference(memberId);
    }

    private UserPreferenceResponse createPreferenceResponse(
            Long id,
            String preferredArea,
            CompanionType companionType,
            MobilityLevel mobilityLevel,
            Boolean avoidCrowded
    ) {
        Map<String, Object> values = new HashMap<>();
        values.put("id", id);
        values.put("preferredArea", preferredArea);
        values.put("categories", List.of());
        values.put("companionType", companionType);
        values.put("mobilityLevel", mobilityLevel);
        values.put("avoidCrowded", avoidCrowded);
        values.put("createdAt", LocalDateTime.now());
        values.put("updatedAt", LocalDateTime.now());

        return createRecord(UserPreferenceResponse.class, values);
    }

    private static <T> T createRecord(Class<T> recordType, Map<String, Object> values) {
        try {
            RecordComponent[] components = recordType.getRecordComponents();

            Class<?>[] parameterTypes = new Class<?>[components.length];
            Object[] args = new Object[components.length];

            for (int i = 0; i < components.length; i++) {
                parameterTypes[i] = components[i].getType();
                args[i] = values.get(components[i].getName());
            }

            Constructor<T> constructor = recordType.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);

            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "테스트용 record 생성에 실패했습니다: " + recordType.getSimpleName(),
                    e
            );
        }
    }
}