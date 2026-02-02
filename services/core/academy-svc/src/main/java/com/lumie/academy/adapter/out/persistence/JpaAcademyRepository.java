package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Academy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaAcademyRepository extends JpaRepository<Academy, Long> {

    Optional<Academy> findByName(String name);

    Page<Academy> findByIsActive(Boolean isActive, Pageable pageable);

    long countByIsActive(Boolean isActive);

    boolean existsByName(String name);
}
