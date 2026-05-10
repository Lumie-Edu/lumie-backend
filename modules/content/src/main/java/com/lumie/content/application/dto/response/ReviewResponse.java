package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        String reviewerName,
        String reviewTitle,
        String reviewContent,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getReviewerName(),
                review.getReviewTitle(),
                review.getReviewContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
