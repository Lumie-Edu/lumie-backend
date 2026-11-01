package com.lumie.content.application.service;

import com.lumie.content.application.dto.response.ReviewResponse;
import com.lumie.content.application.port.out.ReviewPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewQueryService {

    private final ReviewPersistencePort reviewRepository;

    public Page<ReviewResponse> listReviews(Pageable pageable) {
        log.debug("Listing reviews with pagination");
        return reviewRepository.findAll(pageable)
                .map(ReviewResponse::from);
    }
}
