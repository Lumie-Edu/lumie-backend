package com.lumie.assignment.application.port.out;

import java.util.List;

public interface AssignmentRecipientSourcePersistencePort {

    void recordClassSource(Long recipientId, Long assignmentId, Long studentId,
                           Long classTargetId, boolean confirmExisting);

    void recordStudentSource(Long recipientId, Long assignmentId, Long studentId,
                             Long studentTargetId, boolean confirmExisting);

    boolean hasSource(Long recipientId);

    List<Long> findEligibleAssignmentIdsByStudentId(Long studentId);
}
