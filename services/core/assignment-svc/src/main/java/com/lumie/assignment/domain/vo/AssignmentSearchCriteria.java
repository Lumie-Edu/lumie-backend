package com.lumie.assignment.domain.vo;

public record AssignmentSearchCriteria(
    Long classId,
    Long academyId,
    AssignmentType type,
    AssignmentStatus status,
    String search
) {
}
