package com.lumie.exam.domain.repository;

import com.lumie.exam.adapter.out.persistence.AcademyComparisonProjection;
import com.lumie.exam.adapter.out.persistence.ResultWithAcademyProjection;
import com.lumie.exam.domain.entity.ExamResult;

import java.util.List;
import java.util.Optional;

public interface ExamResultRepository {

    ExamResult save(ExamResult result);

    List<ExamResult> saveAll(List<ExamResult> results);

    Optional<ExamResult> findById(Long id);

    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);

    List<ExamResult> findByExamId(Long examId);

    List<ExamResult> findByStudentId(Long studentId);

    void deleteById(Long id);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);

    List<AcademyComparisonProjection> findAcademyComparisonByExamId(Long examId);

    List<ResultWithAcademyProjection> findResultsWithAcademyByExamId(Long examId);
}
