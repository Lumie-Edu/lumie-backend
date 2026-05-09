package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {
}
