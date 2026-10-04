package com.lumie.assignment.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reads the tenant-safe typed target relations. */
public interface AssignmentAudienceReadPort {

    Map<Long, Targets> findByAssignmentIds(Collection<Long> assignmentIds);

    Optional<Long> findClassTargetId(Long assignmentId, Long classId);

    Optional<Long> findStudentTargetId(Long assignmentId, Long studentId);

    default Targets findByAssignmentId(Long assignmentId) {
        return findByAssignmentIds(List.of(assignmentId))
                .getOrDefault(assignmentId, new Targets(List.of(), List.of()));
    }

    record Targets(List<Long> classIds, List<Long> studentIds) {
        public Targets {
            classIds = List.copyOf(classIds);
            studentIds = List.copyOf(studentIds);
        }

        public Long firstClassId() {
            return classIds.isEmpty() ? null : classIds.get(0);
        }
    }
}
