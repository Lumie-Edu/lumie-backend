package com.lumie.assignment.application.service;

import com.lumie.assignment.domain.entity.AssignmentRecipient;
import com.lumie.assignment.domain.repository.AssignmentRecipientRepository;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.classroom.api.ClassService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentRecipientService {

    private final AssignmentRecipientRepository recipientRepository;
    private final AssignmentRepository assignmentRepository;
    private final ClassService classService;

    public void snapshotRecipients(String tenantSlug, Long assignmentId, List<Long> classIds,
                                   List<Long> directStudentIds) {
        Set<Long> recipients = new LinkedHashSet<>(directStudentIds);
        classIds.forEach(classId -> recipients.addAll(classService.getEnrolledStudentIds(tenantSlug, classId)));
        addRecipients(assignmentId, recipients);
    }

    public void addNewClassEnrollees(String tenantSlug, Long classId, List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        assignmentRepository.findActiveByTargetClassId(classId, now).forEach(assignment ->
                addRecipients(assignment.getId(), new LinkedHashSet<>(studentIds)));
    }

    @Transactional(readOnly = true)
    public boolean isRecipient(Long assignmentId, Long studentId) {
        return recipientRepository.existsByAssignmentIdAndStudentId(assignmentId, studentId);
    }

    private void addRecipients(Long assignmentId, Set<Long> studentIds) {
        Set<Long> existing = new LinkedHashSet<>(recipientRepository.findStudentIdsByAssignmentId(assignmentId));
        List<AssignmentRecipient> recipients = studentIds.stream()
                .filter(studentId -> studentId != null && studentId > 0)
                .filter(studentId -> !existing.contains(studentId))
                .map(studentId -> AssignmentRecipient.create(assignmentId, studentId))
                .toList();
        if (!recipients.isEmpty()) {
            recipientRepository.saveAll(recipients);
        }
    }
}
