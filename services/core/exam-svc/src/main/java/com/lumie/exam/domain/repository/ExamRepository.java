package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.Exam;

import java.util.List;
import java.util.Optional;

public interface ExamRepository {

    Exam save(Exam exam);

    Optional<Exam> findById(Long id);

    List<Exam> findAll();

    void deleteById(Long id);

    boolean existsById(Long id);
}
