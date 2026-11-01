package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewJpaRepository extends JpaRepository<Review, Long> {
}
