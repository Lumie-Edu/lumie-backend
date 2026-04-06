package com.lumie.internal.assignment;

import java.util.List;
import java.util.Optional;

public interface AssignmentInternalService {

    Optional<AssignmentData> getAssignment(String tenantSlug, Long assignmentId);

    AssignmentListData getAssignmentsByClass(String tenantSlug, Long classId, int page, int size);

    record AssignmentData(
            Long id,
            String title,
            Long classId,
            Long academyId,
            String type,
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
