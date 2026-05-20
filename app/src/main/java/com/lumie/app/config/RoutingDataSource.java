package com.lumie.app.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routes JDBC connections to the read-only replica pool when the active
 * Spring transaction is marked {@code @Transactional(readOnly = true)},
 * otherwise to the primary read-write pool. Selection happens once per
 * physical connection acquisition; the routing key is recomputed every
 * time {@code getConnection()} is called on the underlying
 * {@link org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy}.
 *
 * <p>Wrap this bean with {@code LazyConnectionDataSourceProxy} in the
 * {@code @Primary} {@code DataSource} bean — Spring's transaction
 * infrastructure calls {@code getConnection()} <em>before</em> setting the
 * {@code currentTransactionReadOnly} flag on
 * {@link TransactionSynchronizationManager}. Without the lazy proxy, the
 * routing key would always resolve to {@link #KEY_PRIMARY}, defeating the
 * routing.
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    static final String KEY_PRIMARY = "primary";
    static final String KEY_READONLY = "readonly";

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                ? KEY_READONLY
                : KEY_PRIMARY;
    }
}
