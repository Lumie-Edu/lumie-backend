package com.lumie.common.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RlsTenantContextAspect {

    private static final String SET_TENANT_SQL = "SELECT set_config('app.tenant_id', ?, true)";

    private final DataSource dataSource;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) "
            + "|| @within(org.springframework.transaction.annotation.Transactional)")
    public Object bindTenantToTransaction(ProceedingJoinPoint pjp) throws Throwable {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            Connection con = DataSourceUtils.getConnection(dataSource);
            try (PreparedStatement ps = con.prepareStatement(SET_TENANT_SQL)) {
                ps.setString(1, String.valueOf(tenantId));
                ps.execute();
            } catch (SQLException e) {
                log.error("Failed to bind RLS tenant_id={} to current transaction", tenantId, e);
                throw e;
            }
        }
        return pjp.proceed();
    }
}
