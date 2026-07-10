package com.lumie.assignment.domain.vo;

public record AssignmentSearchCriteria(
    Long classId,
    AssignmentStatus status
) {
}
