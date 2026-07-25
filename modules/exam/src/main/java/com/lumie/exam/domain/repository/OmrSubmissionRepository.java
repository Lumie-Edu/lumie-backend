package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.OmrSubmission;
import com.lumie.exam.domain.vo.OmrReconciliationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OmrSubmissionRepository extends JpaRepository<OmrSubmission, Long> {

    @EntityGraph(attributePaths = {"exam", "questions"})
    Optional<OmrSubmission> findByJobIdAndImageIndex(Long jobId, Integer imageIndex);

    @EntityGraph(attributePaths = {"exam", "questions"})
    List<OmrSubmission> findByJobIdOrderByImageIndexAsc(Long jobId);

    @EntityGraph(attributePaths = {"exam", "questions"})
    List<OmrSubmission> findByExamIdAndReconciliationStatusInOrderByCreatedAtDesc(
            Long examId, List<OmrReconciliationStatus> statuses);

    @EntityGraph(attributePaths = {"exam", "questions"})
    @Query("""
            select submission from OmrSubmission submission
            where submission.reconciliationStatus = :status
              and ((submission.reconciliationPhoneNumber is not null
                    and submission.reconciliationPhoneNumber = :phoneNumber)
                   or (submission.reconciliationPhoneNumber is null
                    and submission.ocrPhoneNumber = :phoneNumber))
            """)
    List<OmrSubmission> findByEffectivePhoneNumberAndReconciliationStatus(
            @Param("phoneNumber") String phoneNumber,
            @Param("status") OmrReconciliationStatus status);

    @Query("""
            select submission.sourceImageKey from OmrSubmission submission
            where submission.jobId = :jobId
              and submission.gradingStatus = com.lumie.exam.domain.vo.OmrGradingStatus.FAILED
              and submission.reconciliationReason = 'IMAGE_PERSISTENCE_FAILED'
            """)
    List<String> findSourceImageKeysWithoutPermanentCopyByJobId(@Param("jobId") Long jobId);
}
