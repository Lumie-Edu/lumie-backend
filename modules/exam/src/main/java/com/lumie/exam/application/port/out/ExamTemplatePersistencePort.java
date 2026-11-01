package com.lumie.exam.application.port.out;

import com.lumie.exam.domain.entity.ExamTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/** Application-owned exam template persistence operations. */
public interface ExamTemplatePersistencePort {

    Optional<ExamTemplate> findById(Long id);

    List<ExamTemplate> findAll();

    Page<ExamTemplate> findAll(Pageable pageable);

    boolean existsById(Long id);

    ExamTemplate save(ExamTemplate template);

    void deleteById(Long id);
}
