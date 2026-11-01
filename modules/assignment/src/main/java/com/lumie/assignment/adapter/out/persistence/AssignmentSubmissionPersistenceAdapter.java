package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.application.port.out.AssignmentSubmissionPersistencePort;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentSubmissionPersistenceAdapter implements AssignmentSubmissionPersistencePort {
    private final AssignmentSubmissionJpaRepository repository;

    @Override public Page<AssignmentSubmission> findByAssignmentId(Long id, Pageable pageable) { return repository.findByAssignmentId(id, pageable); }
    @Override public List<AssignmentSubmission> findAllByAssignmentId(Long id) { return repository.findAllByAssignmentId(id); }
    @Override public List<AssignmentSubmission> findAllByAssignmentIdIn(Collection<Long> ids) { return repository.findAllByAssignmentIdIn(ids); }
    @Override public Page<AssignmentSubmission> findByStudentId(Long id, Pageable pageable) { return repository.findByStudentId(id, pageable); }
    @Override public Page<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId, Pageable pageable) { return repository.findByAssignmentIdAndStudentId(assignmentId, studentId, pageable); }
    @Override public Optional<AssignmentSubmission> findFirstByAssignmentIdAndStudentId(Long assignmentId, Long studentId) { return repository.findFirstByAssignmentIdAndStudentId(assignmentId, studentId); }
    @Override public boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId) { return repository.existsByAssignmentIdAndStudentId(assignmentId, studentId); }
    @Override public List<Long> findAssignmentIdsByStudentId(Long studentId) { return repository.findAssignmentIdsByStudentId(studentId); }
    @Override public List<Object[]> countGroupedByStatus() { return repository.countGroupedByStatus(); }
    @Override public List<Object[]> countGroupedByStatusSince(Instant from) { return repository.countGroupedByStatusSince(from); }
    @Override public Optional<AssignmentSubmission> findById(Long id) { return repository.findById(id); }
    @Override public AssignmentSubmission save(AssignmentSubmission submission) { return repository.save(submission); }
}
