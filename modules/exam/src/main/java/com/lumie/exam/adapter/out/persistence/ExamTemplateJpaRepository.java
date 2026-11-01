package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ExamTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamTemplateJpaRepository extends JpaRepository<ExamTemplate, Long> {
}
