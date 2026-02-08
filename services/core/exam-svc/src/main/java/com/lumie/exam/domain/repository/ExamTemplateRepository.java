package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.ExamTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ExamTemplateRepository {

    ExamTemplate save(ExamTemplate template);

    Optional<ExamTemplate> findById(Long id);

    Page<ExamTemplate> findAll(Pageable pageable);

    void deleteById(Long id);

    boolean existsById(Long id);
}
