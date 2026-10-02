package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.application.port.out.AssignmentAudienceReadPort;
import com.lumie.assignment.domain.entity.AssignmentClassTarget;
import com.lumie.assignment.domain.entity.AssignmentStudentTarget;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentAudienceReadAdapter implements AssignmentAudienceReadPort {

    private final AssignmentClassTargetJpaRepository classTargets;
    private final AssignmentStudentTargetJpaRepository studentTargets;

    @Override
    public Optional<Long> findClassTargetId(Long assignmentId, Long classId) {
        return classTargets.findByAssignmentIdAndClassId(assignmentId, classId)
                .map(AssignmentClassTarget::getId);
    }

    @Override
    public Optional<Long> findStudentTargetId(Long assignmentId, Long studentId) {
        return studentTargets.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .map(AssignmentStudentTarget::getId);
    }

    @Override
    public Map<Long, Targets> findByAssignmentIds(Collection<Long> assignmentIds) {
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> classesByAssignment = new HashMap<>();
        for (AssignmentClassTarget target : classTargets.findByAssignmentIdIn(assignmentIds)) {
            classesByAssignment.computeIfAbsent(target.getAssignmentId(), ignored -> new ArrayList<>())
                    .add(target.getClassId());
        }

        Map<Long, List<Long>> studentsByAssignment = new HashMap<>();
        for (AssignmentStudentTarget target : studentTargets.findByAssignmentIdIn(assignmentIds)) {
            studentsByAssignment.computeIfAbsent(target.getAssignmentId(), ignored -> new ArrayList<>())
                    .add(target.getStudentId());
        }

        Map<Long, Targets> result = new HashMap<>();
        for (Long assignmentId : assignmentIds) {
            List<Long> classIds = classesByAssignment.getOrDefault(assignmentId, List.of()).stream().sorted().toList();
            List<Long> studentIds = studentsByAssignment.getOrDefault(assignmentId, List.of()).stream().sorted().toList();
            result.put(assignmentId, new Targets(classIds, studentIds));
        }
        return result;
    }
}
