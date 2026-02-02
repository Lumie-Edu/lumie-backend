package com.lumie.content.application.service;

import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;

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
