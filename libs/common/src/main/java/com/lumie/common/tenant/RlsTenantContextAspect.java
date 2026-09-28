package com.lumie.common.tenant;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Issues {@code SELECT set_config('app.tenant_id', ?, true)} at every
 * {@code @Transactional} entry so Postgres RLS policies can isolate rows
 * by tenant.
 *
 * <p><b>Ordering invariant:</b> Spring's {@code TransactionInterceptor}
 * must be pinned to {@code HIGHEST_PRECEDENCE} in {@code JpaConfig}; this
 * aspect at {@code HIGHEST_PRECEDENCE + 1} wraps INSIDE the tx interceptor
 * so {@code set_config(..., is_local=true)} runs after Spring has flipped
 * the connection's {@code autoCommit} to {@code false}. The {@code true}
 * (tx-local) flag means PostgreSQL drops the setting automatically at
 * COMMIT/ROLLBACK — no manual cleanup, transaction-pool PgBouncer
 * compatible.
 *
 * <p>If you ever see "tenant_id setting not visible to RLS policy" or
 * "row not found despite valid tenant context": the FIRST thing to check
 * is that {@code @EnableTransactionManagement(order = HIGHEST_PRECEDENCE)}
 * is still set in {@link com.lumie.app.config.JpaConfig}. Without it, this
 * advice fires before the tx begins (autoCommit still true) and the
 * tx-local setting evaporates after a single statement — the exact post-
 * mortem failure mode that prompted the schema-per-tenant retirement.
 *
 * <p>Cross-tenant administrative paths (scheduler iteration, platform
 * queries on {@code tenants}/{@code users}) must either: (a) leave the
 * holder empty (advice no-ops, RLS policies see NULL setting and behave
 * per their {@code USING} clause — typically empty result), or (b) set a
 * specific tenantId for each iteration.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RlsTenantContextAspect {

    private final RlsTenantTransactionBinder transactionBinder;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) "
            + "|| @within(org.springframework.transaction.annotation.Transactional)")
    public Object bindTenantToTransaction(ProceedingJoinPoint pjp) throws Throwable {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            Long tenantId = TenantContextHolder.getTenantId();
            transactionBinder.bindTenantToCurrentTransaction(tenantId);
        }
        return pjp.proceed();
    }
}
