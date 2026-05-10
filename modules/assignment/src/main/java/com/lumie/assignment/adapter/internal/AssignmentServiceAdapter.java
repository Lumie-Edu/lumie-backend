package com.lumie.assignment.adapter.internal;

import com.lumie.assignment.application.dto.AssignmentResponse;
import com.lumie.assignment.application.service.AssignmentQueryService;
import com.lumie.assignment.domain.exception.AssignmentNotFoundException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.assignment.api.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentServiceAdapter implements AssignmentService {

    private final AssignmentQueryService assignmentQueryService;

    @Override
    public Optional<AssignmentData> getAssignment(String tenantSlug, Long assignmentId) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            try {
                AssignmentResponse response = assignmentQueryService.getAssignment(assignmentId);
                return Optional.of(toAssignmentData(response));
            } catch (AssignmentNotFoundException e) {
                return Optional.<AssignmentData>empty();
            }
        });
    }

    @Override
    public AssignmentListData getAssignmentsByClass(String tenantSlug, Long classId, int page, int size) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            Page<AssignmentResponse> assignments = assignmentQueryService.getAssignmentsByClass(classId,
                    PageRequest.of(page, size));
            return new AssignmentListData(
                    assignments.getContent().stream().map(this::toAssignmentData).toList(),
                    assignments.getTotalElements(), page, size
            );
        });
    }

    private AssignmentData toAssignmentData(AssignmentResponse response) {
        return new AssignmentData(
                response.id(),
                response.title(),
                response.classId(),
                response.type() != null ? response.type().name() : "HOMEWORK",
                response.status() != null ? response.status().name() : "ACTIVE",
                response.createdAt() != null ? response.createdAt().toString() : "",
                response.updatedAt() != null ? response.updatedAt().toString() : ""
        );
    }
}
