package com.lumie.app.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routes JDBC connections to either the primary read-write DataSource or the
 * read-only replica DataSource depending on the active Spring transaction's
 * {@code readOnly} flag.
 *
 * <p>Lookup keys:
 * <ul>
 *   <li>{@link #KEY_READONLY} — when
 *       {@link TransactionSynchronizationManager#isCurrentTransactionReadOnly()}
 *       returns {@code true} (i.e. the method is annotated
 *       {@code @Transactional(readOnly = true)}).</li>
 *   <li>{@link #KEY_PRIMARY} — for all write transactions and any code
 *       running outside a Spring transaction context.</li>
 * </ul>
 *
 * <p>This bean must be wrapped in
 * {@link org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy}
 * before being handed to Hibernate / {@code DataSourceTransactionManager} —
 * see {@link RoutingDataSourceConfig} for the full wiring rationale.
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
