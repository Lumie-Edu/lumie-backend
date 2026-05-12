package com.lumie.common.auth;

/**
 * Audit-log action verbs — what was done to an {@link EntityType}.
 *
 * <p>Lives in {@code libs/common/auth} so callers across all modules share a
 * single vocabulary. Persisted via {@link jakarta.persistence.EnumType#STRING},
 * so adding new values is backward-compatible.
 */
public enum ActionType {
    VIEW,
    CREATE,
    UPDATE,
    DELETE,
    DEACTIVATE,
    REACTIVATE,
    TERMINATE,
    ENROLL,
    DROP,
    SEND,
    GRADE,
    CHECK_IN,
    CLOSE,
    RESET_PASSWORD,
    CHANGE_LOGIN_ID,
}
