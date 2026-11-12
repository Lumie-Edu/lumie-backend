package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.application.port.out.OmrSubmissionPersistencePort;
import com.lumie.exam.domain.entity.OmrSubmission;
import com.lumie.exam.domain.vo.OmrReconciliationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OmrSubmissionPersistenceAdapter implements OmrSubmissionPersistencePort {

    private final OmrSubmissionJpaRepository jpaRepository;

    @Override public Optional<OmrSubmission> findByJobIdAndImageIndex(Long jobId, Integer imageIndex) {
        return jpaRepository.findByJobIdAndImageIndex(jobId, imageIndex);
    }
    @Override public Optional<OmrSubmission> findByJobIdAndImageIndexForUpdate(Long jobId, Integer imageIndex) {
        return jpaRepository.findByJobIdAndImageIndexForUpdate(jobId, imageIndex);
    }
    @Override public List<OmrSubmission> findByJobIdOrderByImageIndexAsc(Long jobId) {
        return jpaRepository.findByJobIdOrderByImageIndexAsc(jobId);
    }
    @Override public List<OmrSubmission> findByExamIdAndReconciliationStatusInOrderByCreatedAtDesc(
            Long examId, List<OmrReconciliationStatus> statuses) {
        return jpaRepository.findByExamIdAndReconciliationStatusInOrderByCreatedAtDesc(examId, statuses);
    }
    @Override public List<OmrSubmission> findSucceededByExamId(Long examId) {
        return jpaRepository.findSucceededByExamId(examId);
    }
    @Override public List<OmrSubmission> findByEffectivePhoneNumberAndReconciliationStatus(
            String phoneNumber, OmrReconciliationStatus status) {
        return jpaRepository.findByEffectivePhoneNumberAndReconciliationStatus(phoneNumber, status);
    }
    @Override public List<String> findSourceImageKeysWithoutPermanentCopyByJobId(Long jobId) {
        return jpaRepository.findSourceImageKeysWithoutPermanentCopyByJobId(jobId);
    }
    @Override public Optional<OmrSubmission> findById(Long id) { return jpaRepository.findById(id); }
    @Override public List<OmrSubmission> saveAll(Iterable<OmrSubmission> submissions) {
        return jpaRepository.saveAll(submissions);
    }
}
