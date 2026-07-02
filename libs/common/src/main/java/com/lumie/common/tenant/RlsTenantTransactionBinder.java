package com.lumie.common.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RlsTenantTransactionBinder {

    private static final String SET_TENANT_SQL = "SELECT set_config('app.tenant_id', ?, true)";

    private final DataSource dataSource;

    public void bindRequiredTenantToCurrentTransaction() {
        bindTenantToCurrentTransaction(TenantContextHolder.getRequiredTenantId());
    }

    public void bindTenantToCurrentTransaction(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("RLS tenant binding requires an active transaction");
        }
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(SET_TENANT_SQL)) {
            statement.setString(1, String.valueOf(tenantId));
            statement.execute();
        } catch (SQLException e) {
            log.error("Failed to bind RLS tenant_id={} to current transaction", tenantId, e);
            throw new DataAccessResourceFailureException(
                    "Failed to bind RLS tenant_id to current transaction", e);
        }
    }
}
