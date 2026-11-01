package com.lumie.exam.application.port.out;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/** Application-owned exam persistence operations. */
public interface ExamPersistencePort {

    Optional<Exam> findById(Long id);

    Optional<Exam> findByIdForUpdate(Long id);

    Page<Exam> findBySource(ExamSource source, Pageable pageable);

    List<Exam> findBySource(ExamSource source);

    Exam save(Exam exam);

    void delete(Exam exam);

    void flush();
}
