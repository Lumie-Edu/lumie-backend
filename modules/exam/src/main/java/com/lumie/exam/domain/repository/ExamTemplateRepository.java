package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.ExamTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamTemplateRepository extends JpaRepository<ExamTemplate, Long> {
}
