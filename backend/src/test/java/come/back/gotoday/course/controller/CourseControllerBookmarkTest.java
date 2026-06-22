package come.back.gotoday.course.controller;

import come.back.gotoday.course.dto.CourseBookmarkResponse;
import come.back.gotoday.course.dto.SavedCoursePageResponse;
import come.back.gotoday.course.dto.SavedCourseResponse;
import come.back.gotoday.course.service.CourseService;
import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseControllerBookmarkTest {

    @InjectMocks
    private CourseController courseController;

    @Mock
    private CourseService courseService;

    @Mock
    private CustomUserDetails userDetails;

    @Test
    @DisplayName("코스 북마크를 등록한다")
    void toggleBookmark_whenBookmarked() {
        // given
        Long memberId = 1L;
        Long courseId = 10L;

        given(userDetails.getMemberId())
                .willReturn(memberId);
        given(courseService.toggleBookmark(memberId, courseId))
                .willReturn(CourseBookmarkResponse.bookmarked(courseId));

        // when
        ResponseEntity<ApiResponse<CourseBookmarkResponse>> response =
                courseController.toggleBookmark(userDetails, courseId);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        ApiResponse<CourseBookmarkResponse> body = response.getBody();
        assertThat(body).isNotNull();

        CourseBookmarkResponse data = getApiResponseField(body, "data");

        assertThat(data.courseId()).isEqualTo(courseId);
        assertThat(data.bookmarked()).isTrue();

        verify(courseService).toggleBookmark(memberId, courseId);
    }

    @Test
    @DisplayName("코스 북마크 여부를 조회한다")
    void isBookmarked() {
        // given
        Long memberId = 1L;
        Long courseId = 10L;

        given(userDetails.getMemberId())
                .willReturn(memberId);
        given(courseService.isBookmarked(memberId, courseId))
                .willReturn(true);

        // when
        ResponseEntity<ApiResponse<Boolean>> response =
                courseController.isBookmarked(userDetails, courseId);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        ApiResponse<Boolean> body = response.getBody();
        assertThat(body).isNotNull();

        Boolean data = getApiResponseField(body, "data");

        assertThat(data).isTrue();

        verify(courseService).isBookmarked(memberId, courseId);
    }

    @Test
    @DisplayName("내가 북마크한 코스 목록을 10개 단위로 조회한다")
    void getSavedCourses() {
        // given
        Long memberId = 1L;
        int page = 0;

        SavedCourseResponse savedCourseResponse = new SavedCourseResponse(
                100L,
                10L,
                "성수 추천 코스",
                "RECOMMENDATION",
                "성수",
                LocalDate.of(2026, 6, 22),
                LocalDateTime.of(2026, 6, 22, 12, 0)
        );

        SavedCoursePageResponse pageResponse = new SavedCoursePageResponse(
                List.of(savedCourseResponse),
                0,
                10,
                1,
                1,
                true,
                true
        );

        given(userDetails.getMemberId())
                .willReturn(memberId);
        given(courseService.getSavedCourses(memberId, page))
                .willReturn(pageResponse);

        // when
        ResponseEntity<ApiResponse<SavedCoursePageResponse>> response =
                courseController.getSavedCourses(userDetails, page);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        ApiResponse<SavedCoursePageResponse> body = response.getBody();
        assertThat(body).isNotNull();

        SavedCoursePageResponse data = getApiResponseField(body, "data");

        assertThat(data.content()).hasSize(1);
        assertThat(data.content().get(0).courseId()).isEqualTo(10L);
        assertThat(data.page()).isEqualTo(0);
        assertThat(data.size()).isEqualTo(10);
        assertThat(data.totalElements()).isEqualTo(1);
        assertThat(data.totalPages()).isEqualTo(1);
        assertThat(data.first()).isTrue();
        assertThat(data.last()).isTrue();

        verify(courseService).getSavedCourses(memberId, page);
    }

    @SuppressWarnings("unchecked")
    private <T> T getApiResponseField(ApiResponse<?> response, String fieldName) {
        return (T) ReflectionTestUtils.getField(response, fieldName);
    }
}