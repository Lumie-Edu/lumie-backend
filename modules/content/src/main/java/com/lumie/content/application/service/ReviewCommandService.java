package com.lumie.content.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.content.application.dto.request.CreateReviewRequest;
import com.lumie.content.application.dto.response.ReviewResponse;
import com.lumie.content.domain.entity.Review;
import com.lumie.internal.student.StudentService;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;
    private final StudentService studentService;

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        Long userId = UserContextHolder.getRequiredUserId();
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        log.info("Creating review for userId: {}", userId);

        String reviewerName = studentService.getStudentsByUserIds(tenantSlug, List.of(userId))
                .students()
                .stream()
                .findFirst()
                .map(StudentService.StudentData::studentName)
                .orElseThrow(() -> new ContentException(ContentErrorCode.STUDENT_NOT_FOUND));

        Review review = Review.create(reviewerName, request.reviewTitle(), request.reviewContent());
        Review saved = reviewRepository.save(review);
        log.info("Review created: {}", saved.getId());

        return ReviewResponse.from(saved);
    }

    @Transactional
    public void deleteReview(Long id) {
        log.info("Deleting review: {}", id);

        if (!reviewRepository.existsById(id)) {
            throw new ContentException(ContentErrorCode.REVIEW_NOT_FOUND);
        }

        reviewRepository.deleteById(id);
        log.info("Review deleted: {}", id);
    }
}
