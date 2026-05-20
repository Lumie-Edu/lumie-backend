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
 *   <li>{@code "readonly"} — when {@link TransactionSynchronizationManager#isCurrentTransactionReadOnly()}
 *       returns {@code true} (i.e. the method is annotated {@code @Transactional(readOnly=true)}).</li>
 *   <li>{@code "primary"} — for all write transactions and any code running
 *       outside a Spring transaction context.</li>
 * </ul>
 *
 * <p><b>Critical: wrap with {@link org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy}.</b>
 * Spring's {@code DataSourceTransactionManager} / Hibernate's connection provider acquire a
 * physical connection at transaction-open time, which is BEFORE Spring's transaction
 * infrastructure has set the {@code readOnly} flag on {@code TransactionSynchronizationManager}.
 * The proxy defers the physical connection acquisition until the first statement, by which
 * point the readOnly flag is already set — allowing this routing to work correctly.
 * See {@link RoutingDataSourceConfig} for the wiring.
 *
 * <p>Dev / local: when {@code RO_DB_HOST} is unset it falls back to {@code DB_HOST},
 * so both keys resolve to the same physical Postgres instance. Routing still runs;
 * behaviour is identical to pre-D.7 for read-write paths. The only observable difference
 * is a second Hikari pool in the connection pool metrics.
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
