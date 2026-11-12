package com.lumie.exam.application.port.out;

import com.lumie.exam.domain.entity.OmrSubmission;
import com.lumie.exam.domain.vo.OmrReconciliationStatus;

import java.util.List;
import java.util.Optional;

/** Application-owned OMR submission persistence operations. */
public interface OmrSubmissionPersistencePort {

    Optional<OmrSubmission> findByJobIdAndImageIndex(Long jobId, Integer imageIndex);

    Optional<OmrSubmission> findByJobIdAndImageIndexForUpdate(Long jobId, Integer imageIndex);

    List<OmrSubmission> findByJobIdOrderByImageIndexAsc(Long jobId);

    List<OmrSubmission> findByExamIdAndReconciliationStatusInOrderByCreatedAtDesc(
            Long examId, List<OmrReconciliationStatus> statuses);

    List<OmrSubmission> findSucceededByExamId(Long examId);

    List<OmrSubmission> findByEffectivePhoneNumberAndReconciliationStatus(
            String phoneNumber, OmrReconciliationStatus status);

    List<String> findSourceImageKeysWithoutPermanentCopyByJobId(Long jobId);

    Optional<OmrSubmission> findById(Long id);

    List<OmrSubmission> saveAll(Iterable<OmrSubmission> submissions);
}
