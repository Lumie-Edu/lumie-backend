package com.lumie.app.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Wires the {@link RoutingDataSource} that automatically directs
 * {@code @Transactional(readOnly=true)} connections to the read-only
 * replica pool and all write / non-transactional paths to the primary pool.
 *
 * <h2>Bean graph</h2>
 * <pre>
 *   spring.datasource.*        → primaryDataSourceProperties → primaryHikariDataSource
 *   readonly-datasource.*      → (manual @Value binding)    → readOnlyHikariDataSource
 *                                         ↘            ↙
 *                                   RoutingDataSource (AbstractRoutingDataSource)
 *                                         ↓
 *                               LazyConnectionDataSourceProxy   ← @Primary DataSource
 * </pre>
 *
 * <h2>Why LazyConnectionDataSourceProxy?</h2>
 * Spring's transaction infrastructure calls {@code DataSource.getConnection()} at
 * transaction-open time, which happens <em>before</em> it sets
 * {@code TransactionSynchronizationManager.currentTransactionReadOnly}. Without the
 * lazy proxy, {@link RoutingDataSource#determineCurrentLookupKey()} is always called
 * while readOnly is still {@code false}, defeating the routing entirely. The proxy
 * defers the actual connection acquisition to the first JDBC statement, by which
 * point the readOnly flag is already set.
 *
 * <h2>RLS continuity</h2>
 * {@link com.lumie.common.tenant.RlsTenantContextAspect} injects this {@code @Primary}
 * {@code DataSource} bean and calls
 * {@code DataSourceUtils.getConnection(dataSource)} inside the active transaction.
 * Because {@code DataSourceUtils} returns the connection that Spring has already
 * bound to the current transaction (via the lazy proxy), the GUC
 * {@code set_config('app.tenant_id', ?, true)} is issued on the same physical
 * connection the query will use — whether that is a primary or read-only connection.
 * No special handling is required; routing is transparent to the RLS aspect.
 *
 * <h2>AI module read-only JdbcTemplate</h2>
 * {@link com.lumie.ai.adapter.out.config.ReadOnlyDataSourceConfig} registers a
 * {@code readOnlyJdbcTemplate} bean backed by its own raw {@code HikariDataSource}.
 * That bean operates <em>outside</em> this routing (it does not go through the
 * {@code @Primary} DataSource) and is unchanged. {@link com.lumie.app.config.JpaConfig}
 * declares a {@code @Primary JdbcTemplate} wired to the routing DataSource so Spring
 * Boot's auto-configured consumers (e.g. Spring Modulith's JDBC event store) get the
 * read-write connection, not the AI module's read-only one.
 *
 * <h2>Dev / local fallback</h2>
 * When {@code RO_DB_HOST} is unset, {@code readonly-datasource.jdbc-url} falls back to
 * the same host as {@code DB_HOST} (see {@code application.yaml}). Both Hikari pools
 * connect to the same Postgres instance. Routing still fires; the only observable effect
 * is a second pool in HikariCP metrics named {@code ReadOnlyHikariPool}.
 *
 * <h2>DataSourceAutoConfiguration back-off</h2>
 * Spring Boot's {@code DataSourceAutoConfiguration} carries
 * {@code @ConditionalOnMissingBean({DataSource.class, XADataSource.class})} at the
 * class level. The {@code @Primary DataSource routingDataSource} bean declared here
 * satisfies that condition, so the autoconfiguration backs off entirely — including
 * its own {@code DataSourceProperties} binding. We therefore drive primary pool
 * binding manually through an explicit {@link DataSourceProperties} bean, which is the
 * standard Spring Boot multi-datasource pattern.
 */
@Configuration
public class RoutingDataSourceConfig {

    // ── Primary DataSource (spring.datasource.*) ──────────────────────────

    /**
     * Binds {@code spring.datasource.*} properties into a {@link DataSourceProperties}
     * bean. Normally this is done by Spring Boot's {@code DataSourceAutoConfiguration},
     * but since we declare a {@code @Primary DataSource} ourselves, autoconfig backs
     * off and we bind manually. The bean name {@code "primaryDataSourceProperties"}
     * avoids any clash with auto-configured beans that Spring may have partially
     * registered on a non-backing-off path.
     */
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Hikari pool for the read-write primary. Hikari-specific pool tuning
     * ({@code maximum-pool-size}, {@code minimum-idle}, etc.) is bound from
     * {@code spring.datasource.hikari.*} via the second
     * {@code @ConfigurationProperties} annotation on this bean.
     *
     * <p>Pool name in JMX / metrics: {@code "PrimaryHikariPool"}.
     */
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource primaryHikariDataSource(DataSourceProperties primaryDataSourceProperties) {
        HikariDataSource ds = primaryDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.setPoolName("PrimaryHikariPool");
        return ds;
    }

    // ── Read-only DataSource (readonly-datasource.*) ──────────────────────

    /**
     * Hikari pool for the read-only replica.
     *
     * <p>The {@code readonly-datasource} YAML stanza uses {@code jdbc-url} (Hikari's
     * native property name) rather than {@code url} (Spring Boot's
     * {@code DataSourceProperties} field name). Using {@link DataSourceProperties}
     * here would lose the property because {@code DataSourceProperties.url} does not
     * bind {@code jdbc-url}. We therefore mirror the AI module's
     * ({@link com.lumie.ai.adapter.out.config.ReadOnlyDataSourceConfig}) hand-built
     * HikariCP approach, using {@code @Value} for each property.
     *
     * <p>{@code setReadOnly(true)} tells the driver and PgBouncer that connections from
     * this pool are read-only, ensuring they can be routed to a standby replica at the
     * network layer even without application-level transaction flags.
     *
     * <p>Pool name in JMX / metrics: {@code "ReadOnlyHikariPool"}.
     *
     * <p>Dev / local: when {@code RO_DB_HOST} is unset the URL resolves to the same
     * host as the primary (application.yaml fallback). This pool is effectively a second
     * connection to the same DB — harmless, and means D.7 routing code can be deployed
     * before a physical replica exists.
     */
    @Bean
    public HikariDataSource readOnlyHikariDataSource(
            @Value("${readonly-datasource.jdbc-url}") String jdbcUrl,
            @Value("${readonly-datasource.username}") String username,
            @Value("${readonly-datasource.password}") String password,
            @Value("${readonly-datasource.driver-class-name}") String driverClassName,
            @Value("${readonly-datasource.maximum-pool-size:5}") int maxPoolSize,
            @Value("${readonly-datasource.minimum-idle:2}") int minIdle,
            @Value("${readonly-datasource.connection-timeout:10000}") long connectionTimeout) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        ds.setMaximumPoolSize(maxPoolSize);
        ds.setMinimumIdle(minIdle);
        ds.setConnectionTimeout(connectionTimeout);
        ds.setReadOnly(true);
        ds.setPoolName("ReadOnlyHikariPool");
        return ds;
    }

    // ── Routing DataSource ────────────────────────────────────────────────

    /**
     * The {@code @Primary DataSource} bean consumed by:
     * <ul>
     *   <li>Spring's {@code JpaTransactionManager} / Hibernate connection provider</li>
     *   <li>{@link com.lumie.common.tenant.RlsTenantContextAspect} (injects DataSource directly)</li>
     *   <li>{@link com.lumie.app.config.ShedLockConfig} (via {@code DataSource} injection)</li>
     *   <li>Spring Modulith JDBC event store (via {@code @Primary JdbcTemplate} in JpaConfig)</li>
     *   <li>Spring Boot Flyway autoconfiguration (uses its own credentials but same URL)</li>
     * </ul>
     *
     * <p>Wrapped with {@link LazyConnectionDataSourceProxy} so the routing key is
     * evaluated at first-statement time (after readOnly is set on
     * {@code TransactionSynchronizationManager}), not at transaction-open time.
     */
    @Bean
    @Primary
    public DataSource routingDataSource(
            HikariDataSource primaryHikariDataSource,
            HikariDataSource readOnlyHikariDataSource) {

        RoutingDataSource routing = new RoutingDataSource();
        routing.setTargetDataSources(Map.of(
                RoutingDataSource.KEY_PRIMARY, primaryHikariDataSource,
                RoutingDataSource.KEY_READONLY, readOnlyHikariDataSource
        ));
        routing.setDefaultTargetDataSource(primaryHikariDataSource);
        routing.afterPropertiesSet();

        // LazyConnectionDataSourceProxy is the load-bearing wrapper.
        // Without it, readOnly routing never fires — see class Javadoc for RoutingDataSource.
        return new LazyConnectionDataSourceProxy(routing);
    }
}
