package come.back.gotoday.review.service;

import come.back.gotoday.course.entity.Course;
import come.back.gotoday.review.dto.ReviewCreateRequest;
import come.back.gotoday.review.dto.ReviewResponse;
import come.back.gotoday.review.dto.ReviewUpdateRequest;
import come.back.gotoday.review.entity.Review;
import come.back.gotoday.review.repository.ReviewRepository;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;

    // 리뷰 생성
    @Transactional
    public ReviewResponse createReview(Long courseId, Long memberId, ReviewCreateRequest request) {
        if (reviewRepository.existsByMemberIdAndCourseId(memberId, courseId)) {
            throw new IllegalArgumentException("이미 해당 코스에 리뷰를 작성했습니다.");
        }
        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스 없음"));

        var member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        Review review = Review.create(
                member,
                course,
                request.getRating(),
                request.getContent());

        reviewRepository.save(review);
        updateCourseRating(course);

        return ReviewResponse.from(review);
    }

    // 리뷰 수정 (본인만 가능)
    @Transactional
    public ReviewResponse updateReview(Long courseId, Long memberId, ReviewUpdateRequest request) {

        Review review = reviewRepository
                .findByCourseIdAndMemberId(courseId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰 없음"));

        // 1. 해당 코스 리뷰 맞는지 검증
        if (!review.getCourse().getId().equals(courseId)) {
            throw new IllegalArgumentException("해당 코스의 리뷰가 아님");
        }

        // 2. 작성자 검증 (핵심)
        if (!review.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인 리뷰만 수정 가능");
        }

        Integer rating = request.getRating();
        String content = request.getContent();

        if (rating == null) rating = review.getRating();
        if (content == null) content = review.getContent();

        review.update(rating, content);
        updateCourseRating(review.getCourse());

        return ReviewResponse.from(review);
    }

    // 리뷰 삭제 (본인만 가능)
    @Transactional
    public void deleteReview(Long courseId, Long memberId) {

        Review review = reviewRepository
                .findByCourseIdAndMemberId(courseId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰 없음"));

        if (!review.getCourse().getId().equals(courseId)) {
            throw new IllegalArgumentException("해당 코스의 리뷰가 아님");
        }

        if (!review.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인 리뷰만 삭제 가능");
        }

        reviewRepository.delete(review);
        updateCourseRating(review.getCourse());
    }

    //단건 리뷰 조회
    public ReviewResponse getReview(Long courseId, Long memberId) {

        Review review = reviewRepository
                .findByCourseIdAndMemberId(courseId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰 없음"));

        return ReviewResponse.from(review);
    }


    //다건 리뷰 조회
    public List<ReviewResponse> getReviews(Long courseId) {
        return reviewRepository.findAllByCourseId(courseId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    private void updateCourseRating(Course course) {
        int count = reviewRepository.countByCourseId(course.getId());
        Double avg = reviewRepository.findAverageRating(course.getId());

        course.updateReviewStats(count, avg == null ? 0.0 : avg);
    }
}