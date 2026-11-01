package com.lumie.content.application.port.out;

import com.lumie.content.domain.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewPersistencePort {
    Review save(Review review);
    boolean existsById(Long id);
    void deleteById(Long id);
    Page<Review> findAll(Pageable pageable);
}
