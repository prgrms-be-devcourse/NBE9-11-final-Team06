package come.back.gotoday.review.service;

import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.review.dto.ReviewCreateRequest;
import come.back.gotoday.review.dto.ReviewResponse;
import come.back.gotoday.review.dto.ReviewUpdateRequest;
import come.back.gotoday.review.entity.Review;
import come.back.gotoday.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MemberRepository memberRepository;

    private Member member;
    private Course course;
    private Review review;

    @BeforeEach
    void setUp() {

        member = Member.create(
                "test@test.com",
                "1234",
                "낄낄",
                "USER",
                "ACTIVE"
        );
        ReflectionTestUtils.setField(member, "id", 1L);

        course = Course.create(
                member,
                "성수 추천 코스",
                "설명",
                "RECOMMENDATION",
                LocalDate.now(),
                LocalDate.now(),
                "성수",
                "FRIEND",
                37.5,
                127.0,
                1200,
                30,
                "추천 이유"
        );
        ReflectionTestUtils.setField(course, "id", 10L);

        review = Review.create(
                member,
                course,
                5,
                "좋아요"
        );
        ReflectionTestUtils.setField(review, "id", 100L);
    }

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReview_success() {

        // given
        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        5,
                        "정말 좋은 코스였습니다."
                );

        given(reviewRepository.existsByMemberIdAndCourseId(1L, 10L))
                .willReturn(false);

        given(courseRepository.findById(10L))
                .willReturn(Optional.of(course));

        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));

        given(reviewRepository.save(any(Review.class)))
                .willAnswer(invocation -> {
                    Review savedReview = invocation.getArgument(0);

                    // JPA가 save() 후 ID를 생성해 준 상황을 흉내냄
                    ReflectionTestUtils.setField(savedReview, "id", 100L);

                    return savedReview;
                });

        given(reviewRepository.countByCourseId(10L))
                .willReturn(1);

        given(reviewRepository.findAverageRating(10L))
                .willReturn(5.0);

        // when
        ReviewResponse response =
                reviewService.createReview(
                        10L,
                        1L,
                        request
                );

        // then
        assertThat(response.reviewId()).isEqualTo(100L);
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.content()).isEqualTo("정말 좋은 코스였습니다.");

        verify(reviewRepository).save(any(Review.class));
        verify(reviewRepository).countByCourseId(10L);
        verify(reviewRepository).findAverageRating(10L);
    }

    @Test
    @DisplayName("이미 작성한 리뷰가 있으면 예외 발생")
    void createReview_duplicate() {

        // given
        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        5,
                        "중복"
                );

        given(reviewRepository.existsByMemberIdAndCourseId(1L, 10L))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                reviewService.createReview(
                        10L,
                        1L,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 해당 코스에 리뷰를 작성했습니다.");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("코스가 없으면 예외 발생")
    void createReview_courseNotFound() {

        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        5,
                        "리뷰"
                );

        given(reviewRepository.existsByMemberIdAndCourseId(1L, 10L))
                .willReturn(false);

        given(courseRepository.findById(10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                reviewService.createReview(
                        10L,
                        1L,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("코스 없음");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("회원이 없으면 예외 발생")
    void createReview_memberNotFound() {

        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        5,
                        "리뷰"
                );

        given(reviewRepository.existsByMemberIdAndCourseId(1L, 10L))
                .willReturn(false);

        given(courseRepository.findById(10L))
                .willReturn(Optional.of(course));

        given(memberRepository.findById(1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                reviewService.createReview(
                        10L,
                        1L,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유저 없음");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReview_success() {

        // given
        ReviewUpdateRequest request =
                new ReviewUpdateRequest(
                        4,
                        "수정된 리뷰입니다."
                );

        given(reviewRepository.findByCourseIdAndMemberId(10L, 1L))
                .willReturn(Optional.of(review));

        given(reviewRepository.countByCourseId(10L))
                .willReturn(1);

        given(reviewRepository.findAverageRating(10L))
                .willReturn(4.0);

        // when
        ReviewResponse response =
                reviewService.updateReview(
                        10L,
                        1L,
                        request
                );

        // then
        assertThat(response.reviewId()).isEqualTo(100L);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.content()).isEqualTo("수정된 리뷰입니다.");

        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getContent()).isEqualTo("수정된 리뷰입니다.");

        verify(reviewRepository).findByCourseIdAndMemberId(10L, 1L);
        verify(reviewRepository).countByCourseId(10L);
        verify(reviewRepository).findAverageRating(10L);
    }

    @Test
    @DisplayName("수정할 리뷰가 없으면 예외 발생")
    void updateReview_reviewNotFound() {

        // given
        ReviewUpdateRequest request =
                new ReviewUpdateRequest(
                        5,
                        "수정"
                );

        given(reviewRepository.findByCourseIdAndMemberId(10L, 1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                reviewService.updateReview(
                        10L,
                        1L,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리뷰 없음");

        verify(reviewRepository).findByCourseIdAndMemberId(10L, 1L);
        verify(reviewRepository, never()).countByCourseId(anyLong());
    }

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_success() {

        // given
        given(reviewRepository.findByCourseIdAndMemberId(10L, 1L))
                .willReturn(Optional.of(review));

        given(reviewRepository.countByCourseId(10L))
                .willReturn(0);

        given(reviewRepository.findAverageRating(10L))
                .willReturn(null);

        willDoNothing().given(reviewRepository).delete(review);

        // when
        reviewService.deleteReview(
                10L,
                1L
        );

        // then
        verify(reviewRepository).findByCourseIdAndMemberId(10L, 1L);
        verify(reviewRepository).delete(review);
        verify(reviewRepository).countByCourseId(10L);
        verify(reviewRepository).findAverageRating(10L);
    }

    @Test
    @DisplayName("삭제할 리뷰가 없으면 예외 발생")
    void deleteReview_reviewNotFound() {

        // given
        given(reviewRepository.findByCourseIdAndMemberId(10L, 1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                reviewService.deleteReview(
                        10L,
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리뷰 없음");

        verify(reviewRepository).findByCourseIdAndMemberId(10L, 1L);
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("리뷰 단건 조회 성공")
    void getReview_success() {

        // given
        given(reviewRepository.findByCourseIdAndMemberId(10L, 1L))
                .willReturn(Optional.of(review));

        // when
        ReviewResponse response =
                reviewService.getReview(
                        10L,
                        1L
                );

        // then
        assertThat(response.reviewId()).isEqualTo(100L);
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.memberNickname()).isEqualTo("낄낄");
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.content()).isEqualTo("좋아요");

        verify(reviewRepository).findByCourseIdAndMemberId(10L, 1L);
    }

    @Test
    @DisplayName("조회할 리뷰가 없으면 예외 발생")
    void getReview_reviewNotFound() {

        // given
        given(reviewRepository.findByCourseIdAndMemberId(10L, 1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                reviewService.getReview(
                        10L,
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리뷰 없음");

        verify(reviewRepository).findByCourseIdAndMemberId(10L, 1L);
    }

    @Test
    @DisplayName("코스 리뷰 목록 조회 성공")
    void getReviews_success() {

        // given
        Review review2 = Review.create(
                member,
                course,
                4,
                "괜찮은 코스였습니다."
        );
        ReflectionTestUtils.setField(review2, "id", 101L);

        given(reviewRepository.findAllByCourseId(10L))
                .willReturn(List.of(review, review2));

        // when
        List<ReviewResponse> responses =
                reviewService.getReviews(10L);

        // then
        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).reviewId()).isEqualTo(100L);
        assertThat(responses.get(0).rating()).isEqualTo(5);
        assertThat(responses.get(0).content()).isEqualTo("좋아요");

        assertThat(responses.get(1).reviewId()).isEqualTo(101L);
        assertThat(responses.get(1).rating()).isEqualTo(4);
        assertThat(responses.get(1).content()).isEqualTo("괜찮은 코스였습니다.");

        verify(reviewRepository).findAllByCourseId(10L);
    }

}