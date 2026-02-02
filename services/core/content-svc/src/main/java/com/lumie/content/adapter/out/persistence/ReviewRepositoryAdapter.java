package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.Review;
import com.lumie.content.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepository {

    private final JpaReviewRepository jpaRepository;

    @Override
    public Review save(Review review) {
        return jpaRepository.save(review);
    }

    @Override
    public Optional<Review> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Review> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
