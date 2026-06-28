package come.back.gotoday.review.dto;

import come.back.gotoday.review.entity.Review;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReviewResponse(
        Long reviewId,
        Long courseId,
        Long memberId,
        String memberNickName,
        Integer rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .courseId(review.getCourse().getId())
                .memberId(review.getMember().getId())
                .memberNickName(review.getMember().getNickname()) // 또는 nickname
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}