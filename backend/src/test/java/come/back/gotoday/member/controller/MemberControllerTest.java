package come.back.gotoday.member.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.member.dto.MemberCreateRequest;
import come.back.gotoday.member.dto.MemberResponse;
import come.back.gotoday.member.dto.MemberUpdateRequest;
import come.back.gotoday.member.service.MemberService;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private MemberService memberService;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private MemberController memberController;

    @Test
    @DisplayName("회원가입에 성공하면 201 Created와 회원 정보를 응답한다")
    void createMember_success() {
        // given
        MemberCreateRequest request = new MemberCreateRequest(
                "test@example.com",
                "password123",
                "낄낄"
        );

        MemberResponse memberResponse = createMemberResponse(
                1L,
                "test@example.com",
                "낄낄",
                null,
                "USER",
                "ACTIVE"
        );

        when(memberService.createMember(request)).thenReturn(memberResponse);

        // when
        ResponseEntity<ApiResponse<MemberResponse>> response =
                memberController.createMember(request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(201);

        ApiResponse<MemberResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("회원가입에 성공했습니다.");
        assertThat(ReflectionTestUtils.getField(body, "data"))
                .isEqualTo(memberResponse);

        verify(memberService).createMember(request);
    }

    @Test
    @DisplayName("내 정보 조회에 성공하면 회원 정보를 응답한다")
    void getMyInfo_success() {
        // given
        Long memberId = 1L;

        MemberResponse memberResponse = createMemberResponse(
                memberId,
                "test@example.com",
                "낄낄",
                null,
                "USER",
                "ACTIVE"
        );

        when(userDetails.getMemberId()).thenReturn(memberId);
        when(memberService.getMyInfo(memberId)).thenReturn(memberResponse);

        // when
        ResponseEntity<ApiResponse<MemberResponse>> response =
                memberController.getMyInfo(userDetails);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ApiResponse<MemberResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("회원 정보를 조회했습니다.");
        assertThat(ReflectionTestUtils.getField(body, "data"))
                .isEqualTo(memberResponse);

        verify(memberService).getMyInfo(memberId);
        verify(userDetails, times(3)).getMemberId();
    }

    @Test
    @DisplayName("내 정보 수정에 성공하면 수정된 회원 정보를 응답한다")
    void updateMyInfo_success() {
        // given
        Long memberId = 1L;

        MemberUpdateRequest request = new MemberUpdateRequest(
                "낄낄수정",
                "https://example.com/profile.png"
        );

        MemberResponse memberResponse = createMemberResponse(
                memberId,
                "test@example.com",
                "낄낄수정",
                "https://example.com/profile.png",
                "USER",
                "ACTIVE"
        );

        when(userDetails.getMemberId()).thenReturn(memberId);
        when(memberService.updateMyInfo(memberId, request)).thenReturn(memberResponse);

        // when
        ResponseEntity<ApiResponse<MemberResponse>> response =
                memberController.updateMyInfo(userDetails, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ApiResponse<MemberResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("회원 정보를 수정했습니다.");
        assertThat(ReflectionTestUtils.getField(body, "data"))
                .isEqualTo(memberResponse);

        verify(memberService).updateMyInfo(memberId, request);
        verify(userDetails, times(3)).getMemberId();
    }

    @Test
    @DisplayName("회원 탈퇴에 성공하면 성공 응답을 반환한다")
    void withdrawMyAccount_success() {
        // given
        Long memberId = 1L;

        when(userDetails.getMemberId()).thenReturn(memberId);
        doNothing().when(memberService).withdrawMyAccount(memberId);

        // when
        ResponseEntity<ApiResponse<Void>> response =
                memberController.withdrawMyAccount(userDetails);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(ReflectionTestUtils.getField(body, "success")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(body, "message"))
                .isEqualTo("회원 탈퇴가 완료되었습니다.");

        verify(memberService).withdrawMyAccount(memberId);
        verify(userDetails, times(3)).getMemberId();
    }

    private MemberResponse createMemberResponse(
            Long id,
            String email,
            String nickname,
            String profileImageUrl,
            String role,
            String status
    ) {
        Map<String, Object> values = new HashMap<>();
        values.put("id", id);
        values.put("email", email);
        values.put("nickname", nickname);
        values.put("profileImageUrl", profileImageUrl);
        values.put("role", role);
        values.put("status", status);
        values.put("createdAt", LocalDateTime.now());
        values.put("updatedAt", LocalDateTime.now());

        return createRecord(MemberResponse.class, values);
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