package com.lumie.content.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "reviewer_name", nullable = false, length = 100)
    private String reviewerName;

    @Column(name = "review_title", nullable = false, length = 200)
    private String reviewTitle;

    @Column(name = "review_content", nullable = false, columnDefinition = "TEXT")
    private String reviewContent;

    @Builder
    private Review(String reviewerName, String reviewTitle, String reviewContent) {
        this.reviewerName = reviewerName;
        this.reviewTitle = reviewTitle;
        this.reviewContent = reviewContent;
    }

    public static Review create(String reviewerName, String reviewTitle, String reviewContent) {
        return Review.builder()
                .reviewerName(reviewerName)
                .reviewTitle(reviewTitle)
                .reviewContent(reviewContent)
                .build();
    }
}
