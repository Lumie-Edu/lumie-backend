package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaExamRepository extends JpaRepository<Exam, Long> {
}
