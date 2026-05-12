package com.lumie.common.auth;

/**
 * Audit-log entity classification — what kind of domain object an action targets.
 *
 * <p>Lives in {@code libs/common/auth} alongside {@link Role} so cross-module
 * callers can reference both as a single cross-cutting vocabulary.
 *
 * <p>The persisted column uses {@link jakarta.persistence.EnumType#STRING},
 * so adding new values is backward-compatible; renaming a value is a
 * breaking data change.
 */
public enum EntityType {
    CLASS,
    STUDENT,
    ADMIN,
    ATTENDANCE,
    ASSIGNMENT,
    EXAM,
    EXAM_TEMPLATE,
    LECTURE,
    ANNOUNCEMENT,
    SMS,
    RESOURCE,
    REVIEW,
    TEXTBOOK,
    ACADEMY,
    SCHEDULE,
    PERMISSION,
    ENROLLMENT,
    RESERVATION,
    QNA,
    CONVERSATION,
    CHAT,
    FILE,
    SCHEDULED_TASK,
    STATISTICS,
    REPORT,
    RESULT,
}
