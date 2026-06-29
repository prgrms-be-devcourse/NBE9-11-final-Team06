package come.back.gotoday.review.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.review.dto.ReviewCreateRequest;
import come.back.gotoday.review.dto.ReviewResponse;
import come.back.gotoday.review.dto.ReviewUpdateRequest;
import come.back.gotoday.review.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @InjectMocks
    private ReviewController reviewController;

    @Mock
    private ReviewService reviewService;

    @Mock
    private CustomUserDetails userDetails;

    @Test
    @DisplayName("리뷰를 생성한다")
    void createReview() {
        // given
        Long courseId = 1L;
        Long memberId = 1L;

        ReviewCreateRequest request =
                new ReviewCreateRequest(5, "좋은 코스였습니다.");

        ReviewResponse reviewResponse = createReviewResponse(
                1L,
                courseId,
                memberId,
                "낄낄",
                5,
                "좋은 코스였습니다."
        );

        given(userDetails.getMemberId()).willReturn(memberId);
        given(reviewService.createReview(courseId, memberId, request))
                .willReturn(reviewResponse);

        // when
        ResponseEntity<ApiResponse<ReviewResponse>> response =
                reviewController.createReview(courseId, request, userDetails);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        ApiResponse<ReviewResponse> body = response.getBody();
        assertThat(body).isNotNull();

        ReviewResponse data = getApiResponseField(body, "data");

        assertThat(data.reviewId()).isEqualTo(1L);
        assertThat(data.rating()).isEqualTo(5);
        assertThat(data.content()).isEqualTo("좋은 코스였습니다.");

        verify(reviewService).createReview(courseId, memberId, request);
    }

    @Test
    @DisplayName("내 리뷰를 조회한다")
    void getReview() {
        Long courseId = 1L;
        Long memberId = 1L;

        ReviewResponse reviewResponse = createReviewResponse(
                1L,
                courseId,
                memberId,
                "낄낄",
                5,
                "좋은 코스였습니다."
        );

        given(userDetails.getMemberId()).willReturn(memberId);
        given(reviewService.getReview(courseId, memberId))
                .willReturn(reviewResponse);

        ResponseEntity<ApiResponse<ReviewResponse>> response =
                reviewController.getReview(courseId, userDetails);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        ReviewResponse data =
                getApiResponseField(response.getBody(), "data");

        assertThat(data.reviewId()).isEqualTo(1L);

        verify(reviewService).getReview(courseId, memberId);
    }

    @Test
    @DisplayName("코스 리뷰 목록을 조회한다")
    void getReviews() {
        Long courseId = 1L;

        List<ReviewResponse> reviews = List.of(
                createReviewResponse(
                        1L,
                        courseId,
                        1L,
                        "낄낄",
                        5,
                        "좋아요"
                ),
                createReviewResponse(
                        2L,
                        courseId,
                        2L,
                        "철수",
                        4,
                        "괜찮아요"
                )
        );

        given(reviewService.getReviews(courseId))
                .willReturn(reviews);

        ResponseEntity<ApiResponse<List<ReviewResponse>>> response =
                reviewController.getReviews(courseId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        List<ReviewResponse> data =
                getApiResponseField(response.getBody(), "data");

        assertThat(data).hasSize(2);

        verify(reviewService).getReviews(courseId);
    }

    @Test
    @DisplayName("리뷰를 수정한다")
    void updateReview() {
        Long courseId = 1L;
        Long memberId = 1L;

        ReviewUpdateRequest request =
                new ReviewUpdateRequest(4, "수정된 리뷰");

        ReviewResponse reviewResponse = createReviewResponse(
                1L,
                courseId,
                memberId,
                "낄낄",
                4,
                "수정된 리뷰"
        );

        given(userDetails.getMemberId()).willReturn(memberId);
        given(reviewService.updateReview(courseId, memberId, request))
                .willReturn(reviewResponse);

        ResponseEntity<ApiResponse<ReviewResponse>> response =
                reviewController.updateReview(courseId, request, userDetails);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        ReviewResponse data =
                getApiResponseField(response.getBody(), "data");

        assertThat(data.rating()).isEqualTo(4);

        verify(reviewService).updateReview(courseId, memberId, request);
    }

    @Test
    @DisplayName("리뷰를 삭제한다")
    void deleteReview() {
        Long courseId = 1L;
        Long memberId = 1L;

        given(userDetails.getMemberId()).willReturn(memberId);
        doNothing().when(reviewService).deleteReview(courseId, memberId);

        ResponseEntity<ApiResponse<Void>> response =
                reviewController.deleteReview(courseId, userDetails);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        verify(reviewService).deleteReview(courseId, memberId);
    }

    @SuppressWarnings("unchecked")
    private <T> T getApiResponseField(ApiResponse<?> response, String fieldName) {
        return (T) ReflectionTestUtils.getField(response, fieldName);
    }

    private ReviewResponse createReviewResponse(
            Long reviewId,
            Long courseId,
            Long memberId,
            String memberNickname,
            Integer rating,
            String content
    ) {
        return ReviewResponse.builder()
                .reviewId(reviewId)
                .courseId(courseId)
                .memberId(memberId)
                .memberNickname(memberNickname)
                .rating(rating)
                .content(content)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}