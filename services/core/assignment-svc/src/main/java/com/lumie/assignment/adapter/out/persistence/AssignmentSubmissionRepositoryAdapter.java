package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AssignmentSubmissionRepositoryAdapter implements AssignmentSubmissionRepository {

    private final JpaAssignmentSubmissionRepository jpaRepository;

    @Override
    public AssignmentSubmission save(AssignmentSubmission submission) {
        return jpaRepository.save(submission);
    }

    @Override
    public Optional<AssignmentSubmission> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<AssignmentSubmission> findByAssignmentId(Long assignmentId, Pageable pageable) {
        return jpaRepository.findByAssignmentId(assignmentId, pageable);
    }

    @Override
    public Page<AssignmentSubmission> findByStudentId(Long studentId, Pageable pageable) {
        return jpaRepository.findByStudentId(studentId, pageable);
    }

    @Override
    public Page<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId, Pageable pageable) {
        return jpaRepository.findByAssignmentIdAndStudentId(assignmentId, studentId, pageable);
    }

    @Override
    public boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId) {
        return jpaRepository.existsByAssignmentIdAndStudentId(assignmentId, studentId);
    }
}
