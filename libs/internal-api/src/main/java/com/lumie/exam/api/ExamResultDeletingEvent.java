package com.lumie.exam.api;

/** Dependents must be removed before the result is deleted, in the same transaction. */
public record ExamResultDeletingEvent(Long tenantId, Long examId, Long resultId) {
}
