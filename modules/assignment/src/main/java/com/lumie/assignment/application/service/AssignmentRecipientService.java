package com.lumie.assignment.application.service;

import com.lumie.assignment.domain.entity.AssignmentRecipient;
import com.lumie.assignment.domain.repository.AssignmentRecipientRepository;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.application.port.out.AssignmentAudienceReadPort;
import com.lumie.assignment.application.port.out.AssignmentRecipientSourcePersistencePort;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.classroom.api.ClassService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final AssignmentAudienceReadPort audienceReadPort;
    private final AssignmentRecipientSourcePersistencePort sourcePort;

    public void snapshotRecipients(String tenantSlug, Long assignmentId, List<Long> classIds,
                                   List<Long> directStudentIds) {
        Map<Long, List<Long>> studentsByClass = new HashMap<>();
        Set<Long> allStudentIds = new LinkedHashSet<>(directStudentIds);
        for (Long classId : classIds) {
            List<Long> studentIds = classService.getEnrolledStudentIds(tenantSlug, classId);
            studentsByClass.put(classId, studentIds);
            allStudentIds.addAll(studentIds);
        }

        RecipientRoster roster = ensureRecipients(assignmentId, allStudentIds);
        for (Long studentId : directStudentIds) {
            Long targetId = audienceReadPort.findStudentTargetId(assignmentId, studentId)
                    .orElseThrow(() -> new BusinessException(AssignmentErrorCode.STUDENT_NOT_FOUND));
            sourcePort.recordStudentSource(roster.byStudentId().get(studentId).getId(), assignmentId,
                    studentId, targetId, roster.newStudentIds().contains(studentId));
        }
        for (Map.Entry<Long, List<Long>> entry : studentsByClass.entrySet()) {
            Long targetId = audienceReadPort.findClassTargetId(assignmentId, entry.getKey())
                    .orElseThrow(() -> new BusinessException(AssignmentErrorCode.CLASS_NOT_FOUND));
            for (Long studentId : entry.getValue()) {
                sourcePort.recordClassSource(roster.byStudentId().get(studentId).getId(), assignmentId,
                        studentId, targetId, roster.newStudentIds().contains(studentId));
            }
        }
    }

    public void addNewClassEnrollees(String tenantSlug, Long classId, List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        assignmentRepository.findActiveByTargetClassId(classId, now).forEach(assignment -> {
            RecipientRoster roster = ensureRecipients(assignment.getId(), new LinkedHashSet<>(studentIds));
            Long targetId = audienceReadPort.findClassTargetId(assignment.getId(), classId)
                    .orElseThrow(() -> new BusinessException(AssignmentErrorCode.CLASS_NOT_FOUND));
            for (Long studentId : studentIds) {
                sourcePort.recordClassSource(roster.byStudentId().get(studentId).getId(), assignment.getId(),
                        studentId, targetId, roster.newStudentIds().contains(studentId));
            }
        });
    }

    @Transactional(readOnly = true)
    public boolean isRecipient(Long assignmentId, Long studentId) {
        return recipientRepository.existsByAssignmentIdAndStudentId(assignmentId, studentId);
    }

    @Transactional(readOnly = true)
    public boolean canSubmit(Long assignmentId, Long studentId) {
        return eligibleRecipientId(assignmentId, studentId).isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<Long> eligibleRecipientId(Long assignmentId, Long studentId) {
        return recipientRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .filter(recipient -> sourcePort.hasSource(recipient.getId()))
                .map(AssignmentRecipient::getId);
    }

    @Transactional(readOnly = true)
    public List<Long> findEligibleAssignmentIds(Long studentId) {
        return sourcePort.findEligibleAssignmentIdsByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public String rosterAccuracy(Long assignmentId) {
        return recipientRepository.existsByAssignmentIdAndSourceProvenance(assignmentId, "UNVERIFIED")
                ? "UNVERIFIED" : "EXACT";
    }

    private RecipientRoster ensureRecipients(Long assignmentId, Set<Long> studentIds) {
        Map<Long, AssignmentRecipient> byStudentId = new HashMap<>();
        for (AssignmentRecipient recipient : recipientRepository.findByAssignmentId(assignmentId)) {
            if (recipient.getStudentId() != null) {
                byStudentId.put(recipient.getStudentId(), recipient);
            }
        }
        Set<Long> newStudentIds = new HashSet<>();
        List<AssignmentRecipient> toCreate = new ArrayList<>();
        for (Long studentId : studentIds) {
            if (studentId != null && studentId > 0 && !byStudentId.containsKey(studentId)) {
                toCreate.add(AssignmentRecipient.create(assignmentId, studentId));
                newStudentIds.add(studentId);
            }
        }
        for (AssignmentRecipient saved : recipientRepository.saveAll(toCreate)) {
            byStudentId.put(saved.getStudentId(), saved);
        }
        return new RecipientRoster(byStudentId, newStudentIds);
    }

    private record RecipientRoster(Map<Long, AssignmentRecipient> byStudentId, Set<Long> newStudentIds) {}
}
