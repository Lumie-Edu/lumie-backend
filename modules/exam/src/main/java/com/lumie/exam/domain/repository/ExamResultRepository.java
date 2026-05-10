package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.ExamResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);

    Optional<ExamResult> findByExamIdAndPhoneNumber(Long examId, String phoneNumber);

    @EntityGraph(attributePaths = {"exam"})
    List<ExamResult> findByExamId(Long examId);

    /**
     * Batch-fetch all results across multiple exams. Used to avoid N+1 queries
     * when computing per-exam statistics for a student's history.
     */
    List<ExamResult> findByExamIdIn(Collection<Long> examIds);

    List<ExamResult> findByExamIdAndStudentIdIsNotNull(Long examId);

    @EntityGraph(attributePaths = {"exam"})
    List<ExamResult> findByStudentId(Long studentId);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ExamResult r
           SET r.studentId = :studentId
         WHERE r.phoneNumber = :phoneNumber
           AND r.studentId IS NULL
        """)
    int linkUnregisteredByPhone(@Param("phoneNumber") String phoneNumber,
                                @Param("studentId") Long studentId);
}
