package com.lumie.assignment.domain.vo;

import java.time.Instant;

public record AssignmentSearchCriteria(
    Long classId,
    AssignmentStatus status,
    Instant now
) {
}
