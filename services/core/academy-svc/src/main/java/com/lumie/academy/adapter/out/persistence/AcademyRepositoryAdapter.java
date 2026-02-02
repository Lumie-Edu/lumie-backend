package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Academy;
import com.lumie.academy.domain.repository.AcademyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AcademyRepositoryAdapter implements AcademyRepository {

    private final JpaAcademyRepository jpaAcademyRepository;

    @Override
    public Academy save(Academy academy) {
        return jpaAcademyRepository.save(academy);
    }

    @Override
    public Optional<Academy> findById(Long id) {
        return jpaAcademyRepository.findById(id);
    }

    @Override
    public Optional<Academy> findByName(String name) {
        return jpaAcademyRepository.findByName(name);
    }

    @Override
    public Page<Academy> findAll(Pageable pageable) {
        return jpaAcademyRepository.findAll(pageable);
    }

    @Override
    public Page<Academy> findByIsActive(Boolean isActive, Pageable pageable) {
        return jpaAcademyRepository.findByIsActive(isActive, pageable);
    }

    @Override
    public long count() {
        return jpaAcademyRepository.count();
    }

    @Override
    public long countByIsActive(Boolean isActive) {
        return jpaAcademyRepository.countByIsActive(isActive);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaAcademyRepository.existsByName(name);
    }

    @Override
    public void delete(Academy academy) {
        jpaAcademyRepository.delete(academy);
    }
}
