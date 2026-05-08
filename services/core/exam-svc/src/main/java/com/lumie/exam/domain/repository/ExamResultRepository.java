package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.ExamResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamResultRepository {

    ExamResult save(ExamResult result);

    List<ExamResult> saveAll(List<ExamResult> results);

    Optional<ExamResult> findById(Long id);

    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);

    Optional<ExamResult> findByExamIdAndPhoneNumber(Long examId, String phoneNumber);

    List<ExamResult> findByExamId(Long examId);

    /**
     * Batch-fetch all results across multiple exams. Used to avoid N+1 queries
     * when computing per-exam statistics for a student's history.
     */
    List<ExamResult> findByExamIdIn(Collection<Long> examIds);

    List<ExamResult> findByExamIdAndStudentIdIsNotNull(Long examId);

    List<ExamResult> findByStudentId(Long studentId);

    void deleteById(Long id);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);

    int linkUnregisteredByPhone(String phoneNumber, Long studentId);
}
