package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReviewRepository {

    Review save(Review review);

    Optional<Review> findById(Long id);

    Page<Review> findAll(Pageable pageable);

    void deleteById(Long id);

    boolean existsById(Long id);
}
