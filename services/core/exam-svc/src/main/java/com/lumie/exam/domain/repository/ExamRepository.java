package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.Exam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ExamRepository {

    Exam save(Exam exam);

    Optional<Exam> findById(Long id);

    List<Exam> findAll();

    Page<Exam> findAll(Pageable pageable);

    void deleteById(Long id);

    boolean existsById(Long id);
}
