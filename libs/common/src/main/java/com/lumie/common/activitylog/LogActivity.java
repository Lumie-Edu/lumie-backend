package com.lumie.common.activitylog;

import com.lumie.common.auth.ActionType;
import com.lumie.common.auth.EntityType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative audit-log annotation.
 *
 * <p>Applied to a {@code @Transactional} command-service method, {@link LogActivityAspect}
 * intercepts the successful return and delegates to {@link com.lumie.activitylog.api.ActivityLogService}
 * — resolving actor from {@link com.lumie.common.tenant.UserContextHolder} and tenant from
 * {@link com.lumie.common.tenant.TenantContextHolder}.
 *
 * <h3>Entity ID resolution (priority order)</h3>
 * <ol>
 *   <li>If {@link #entityIdArg()} is non-empty: the named parameter of the annotated method.</li>
 *   <li>Otherwise: the return value's {@code getId()} (if the return type exposes it).</li>
 *   <li>If neither resolves: {@code null} is forwarded — the log entry is still written.</li>
 * </ol>
 *
 * <h3>Entity name / description</h3>
 * Both default to {@code ""} (forwarded as {@code null} to the service). Override via
 * {@link #entityName()} (static string — useful for constants) or leave blank to let the
 * service adapter omit it.
 *
 * <h3>Ordering invariant</h3>
 * {@link LogActivityAspect} is {@code @Order(HIGHEST_PRECEDENCE + 2)} — INSIDE
 * {@code RlsTenantContextAspect} ({@code HIGHEST_PRECEDENCE + 1}) which is itself INSIDE
 * Spring's {@code TransactionInterceptor} ({@code HIGHEST_PRECEDENCE}). This guarantees the
 * log write happens within the active transaction and sees the correct RLS tenant context.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogActivity {

    /** The audit verb — what was done. */
    ActionType action();

    /** The kind of domain object the action targets. */
    EntityType entity();

    /**
     * Name of the method parameter that holds the entity's primary-key {@code Long}.
     * Leave blank to fall back to {@code returnValue.getId()}.
     */
    String entityIdArg() default "";

    /**
     * Optional static entity name to include in the log entry.
     * Leave blank when the name is dynamic (aspect will pass {@code null}).
     */
    String entityName() default "";
}
