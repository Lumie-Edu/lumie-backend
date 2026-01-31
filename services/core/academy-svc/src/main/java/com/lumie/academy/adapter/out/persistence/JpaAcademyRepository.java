package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Academy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JpaAcademyRepository extends JpaRepository<Academy, Long> {

    Optional<Academy> findByName(String name);

    @Query("SELECT a FROM Academy a WHERE a.isDefault = true")
    Optional<Academy> findDefaultAcademy();

    Page<Academy> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    boolean existsByName(String name);
}
