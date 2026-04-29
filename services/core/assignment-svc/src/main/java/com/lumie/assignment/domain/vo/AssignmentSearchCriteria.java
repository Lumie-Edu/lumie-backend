package com.lumie.assignment.domain.vo;

public record AssignmentSearchCriteria(
    Long classId,
    AssignmentType type,
    AssignmentStatus status,
    String search
) {
}
