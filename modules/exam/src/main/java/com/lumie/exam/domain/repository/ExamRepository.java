package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    Page<Exam> findBySource(ExamSource source, Pageable pageable);

    List<Exam> findBySource(ExamSource source);
}
