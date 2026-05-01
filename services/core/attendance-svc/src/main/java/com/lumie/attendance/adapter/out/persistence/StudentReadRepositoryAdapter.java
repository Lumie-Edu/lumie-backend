package com.lumie.attendance.adapter.out.persistence;

import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StudentReadRepositoryAdapter implements StudentReadRepository {

    private final JpaStudentReadRepository jpaRepository;

    @Override
    public Optional<StudentReadModel> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<StudentReadModel> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public List<StudentReadModel> findByIsActiveTrue() {
        return jpaRepository.findByIsActiveTrue();
    }

    @Override
    public List<StudentReadModel> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return jpaRepository.findAllById(ids);
    }
}
