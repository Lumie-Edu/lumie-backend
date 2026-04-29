package com.lumie.attendance.domain.repository;

import com.lumie.attendance.domain.entity.StudentReadModel;

import java.util.List;
import java.util.Optional;

public interface StudentReadRepository {

    Optional<StudentReadModel> findById(Long id);

    Optional<StudentReadModel> findByUserId(Long userId);

    List<StudentReadModel> findByIsActiveTrue();
}
