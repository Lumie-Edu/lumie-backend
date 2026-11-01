package com.lumie.assignment.application.port.out;

import com.lumie.assignment.domain.entity.AssignmentRecipient;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssignmentRecipientPersistencePort {
    List<AssignmentRecipient> findByAssignmentId(Long assignmentId);
    List<AssignmentRecipient> findByAssignmentIdIn(Collection<Long> assignmentIds);
    boolean existsByAssignmentIdAndSourceProvenance(Long assignmentId, String sourceProvenance);
    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    Optional<AssignmentRecipient> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    List<Long> findAssignmentIdsByStudentId(Long studentId);
    List<Long> findStudentIdsByAssignmentId(Long assignmentId);
    List<Long> findAssignmentIdsByStudentIdIn(Collection<Long> studentIds);
    List<AssignmentRecipient> saveAll(Collection<AssignmentRecipient> recipients);
}
