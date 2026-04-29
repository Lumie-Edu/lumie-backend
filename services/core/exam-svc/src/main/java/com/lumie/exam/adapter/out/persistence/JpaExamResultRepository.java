package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaExamResultRepository extends JpaRepository<ExamResult, Long> {

    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);

    Optional<ExamResult> findByExamIdAndPhoneNumber(Long examId, String phoneNumber);

    List<ExamResult> findByExamId(Long examId);

    List<ExamResult> findByExamIdAndStudentIdIsNotNull(Long examId);

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
