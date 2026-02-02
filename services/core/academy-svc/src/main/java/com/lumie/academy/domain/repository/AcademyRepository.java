package com.lumie.academy.domain.repository;

import com.lumie.academy.domain.entity.Academy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AcademyRepository {

    Academy save(Academy academy);

    Optional<Academy> findById(Long id);

    Optional<Academy> findByName(String name);

    Page<Academy> findAll(Pageable pageable);

    Page<Academy> findByIsActive(Boolean isActive, Pageable pageable);

    long count();

    long countByIsActive(Boolean isActive);

    boolean existsByName(String name);

    void delete(Academy academy);
}
