package com.lumie.common.activitylog;

import com.lumie.activitylog.api.ActivityLogService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * AOP advice that fires the activity log after a method annotated with
 * {@link LogActivity} returns successfully.
 *
 * <h3>Ordering invariant</h3>
 * {@code @Order(HIGHEST_PRECEDENCE + 2)} places this advice INSIDE
 * {@link com.lumie.common.tenant.RlsTenantContextAspect} ({@code HIGHEST_PRECEDENCE + 1}),
 * which is itself INSIDE Spring's {@code TransactionInterceptor}
 * ({@code HIGHEST_PRECEDENCE}). Logging therefore runs inside the active
 * transaction with the correct RLS tenant context already bound.
 *
 * <h3>Failure policy</h3>
 * Any exception thrown by the logging call is caught and logged at WARN
 * level. The advice never rethrows — a logging failure must never abort
 * the business transaction.
 *
 * <h3>Dedup</h3>
 * {@code ActivityLogServiceAdapter.log()} is the primary authority that sets
 * {@link ActivityLogContext#markLogged()} on every write (manual or aspect
 * path) so the fallback {@code ActivityLogInterceptor} skips the HTTP
 * request. This aspect ALSO calls {@code markLogged()} after a successful
 * log — an intentional, idempotent, defensive mark that keeps the dedup
 * contract holding independently of adapter internals and allows this
 * aspect to be unit-tested against a mocked {@code ActivityLogService}.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class LogActivityAspect {

    private final ActivityLogService activityLogService;

    @AfterReturning(
            pointcut = "@annotation(logActivity)",
            returning = "returnValue"
    )
    public void afterReturning(JoinPoint joinPoint, LogActivity logActivity, Object returnValue) {
        try {
            Long tenantId = TenantContextHolder.getTenantId();
            String tenantSlug = TenantContextHolder.getTenant();
            if (tenantId == null || tenantSlug == null) {
                log.warn("[LogActivityAspect] Tenant context not set for method {}; skipping activity log",
                        joinPoint.getSignature().getName());
                return;
            }

            Long actorId = UserContextHolder.getUserId();
            if (actorId == null) {
                log.warn("[LogActivityAspect] User context not set for method {}; skipping activity log",
                        joinPoint.getSignature().getName());
                return;
            }

            // actor_name is NOT NULL in the DB; older tokens may carry no name claim.
            String actorName = UserContextHolder.getUserName();
            if (actorName == null) {
                actorName = "user:" + actorId;
            }
            com.lumie.common.auth.Role role = UserContextHolder.getUserRole();
            String actorRole = role == null ? null : role.name();

            Long entityId = resolveEntityId(joinPoint, logActivity, returnValue);
            String entityName = logActivity.entityName().isBlank() ? null : logActivity.entityName();

            activityLogService.log(
                    actorId, actorName, actorRole,
                    logActivity.action().name(),
                    logActivity.entity().name(),
                    entityId, entityName, null
            );
            // Defensive, idempotent mark. ActivityLogServiceAdapter.log() is
            // the primary authority and already set this; marking again here
            // keeps the dedup contract independent of adapter internals and
            // lets this aspect be unit-tested with a mocked service.
            ActivityLogContext.markLogged();

        } catch (Exception e) {
            log.warn("[LogActivityAspect] Failed to log activity for method {}: {}",
                    joinPoint.getSignature().getName(), e.getMessage());
        }
    }

    private Long resolveEntityId(JoinPoint joinPoint, LogActivity annotation, Object returnValue) {
        String entityIdArg = annotation.entityIdArg();
        if (!entityIdArg.isBlank()) {
            return resolveFromArg(joinPoint, entityIdArg);
        }
        return resolveFromReturnValue(returnValue);
    }

    // Relies on parameter-name reflection: requires the `-parameters` javac
    // flag (set globally in the root build.gradle.kts). If that flag is ever
    // removed, params[i].getName() returns arg0/arg1 and entityIdArg lookups
    // silently fall through to null.
    private Long resolveFromArg(JoinPoint joinPoint, String paramName) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        Parameter[] params = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < params.length; i++) {
            if (paramName.equals(params[i].getName()) && args[i] instanceof Long id) {
                return id;
            }
        }
        log.warn("[LogActivityAspect] entityIdArg='{}' not found or not a Long on method {}",
                paramName, method.getName());
        return null;
    }

    private Long resolveFromReturnValue(Object returnValue) {
        if (returnValue == null) {
            return null;
        }
        // Support POJO getId() AND Java record id() accessors — response
        // DTOs are records (accessor is id(), not getId()).
        for (String accessor : new String[]{"getId", "id"}) {
            try {
                Method m = returnValue.getClass().getMethod(accessor);
                Object id = m.invoke(returnValue);
                if (id instanceof Long l) {
                    return l;
                }
            } catch (NoSuchMethodException ignored) {
                // try the next accessor convention
            } catch (Exception e) {
                log.warn("[LogActivityAspect] Could not invoke {}() on return value {}: {}",
                        accessor, returnValue.getClass().getSimpleName(), e.getMessage());
            }
        }
        return null;
    }
}
