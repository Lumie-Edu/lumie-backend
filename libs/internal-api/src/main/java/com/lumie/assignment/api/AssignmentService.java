package com.lumie.assignment.api;

import java.util.List;
import java.util.Optional;

public interface AssignmentService {

    Optional<AssignmentData> getAssignment(String tenantSlug, Long assignmentId);

    AssignmentListData getAssignmentsByClass(String tenantSlug, Long classId, int page, int size);

    record AssignmentData(
            Long id,
            String examName,
            Long classId,
            String status,
            String createdAt,
            String updatedAt
    ) {}

    record AssignmentListData(
            List<AssignmentData> assignments,
            long totalCount,
            int page,
            int size
    ) {}
}
