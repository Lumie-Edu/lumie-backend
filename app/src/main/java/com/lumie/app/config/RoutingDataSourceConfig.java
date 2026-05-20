package com.lumie.app.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Wires the {@link RoutingDataSource} that automatically directs
 * {@code @Transactional(readOnly = true)} connections to the read-only
 * replica pool and all write / non-transactional paths to the primary pool.
 *
 * <h2>Why every bean below is declared explicitly</h2>
 * <p>The first D.7 attempt (reverted in {@code e9501a8}) only provided a
 * {@code @Primary DataSource} and relied on Spring Boot's
 * {@code JpaAutoConfiguration} to wire {@code EntityManagerFactory} and
 * {@code PlatformTransactionManager} on top of it. That broke startup:
 * Spring Boot's auto-configured EMF kept a transitive dependency on the
 * autoconfigured (now superseded) DataSource bean ordering, surfacing as
 * an unresolved {@code jpaSharedEM_entityManagerFactory} reference.
 *
 * <p>This rewrite supplies the full JPA stack explicitly so nothing is
 * left to autoconfig: the routing DataSource, the EMF that uses it, and
 * the {@link JpaTransactionManager} that uses that EMF.
 *
 * <h2>Bean graph</h2>
 * <pre>
 *   spring.datasource.*       → primaryDataSourceProperties → primaryHikariDataSource
 *   readonly-datasource.*     → (manual @Value binding)     → readOnlyHikariDataSource
 *                                       ↘            ↙
 *                              routingDataSource (AbstractRoutingDataSource, two keys)
 *                                       ↓
 *                          LazyConnectionDataSourceProxy   ← @Primary DataSource
 *                                       ↓
 *                          @Primary LocalContainerEntityManagerFactoryBean
 *                                       ↓
 *                          @Primary JpaTransactionManager
 * </pre>
 *
 * <h2>Why LazyConnectionDataSourceProxy</h2>
 * <p>Spring's transaction infrastructure calls
 * {@code DataSource.getConnection()} at transaction-open time, which
 * happens <em>before</em> it sets
 * {@code TransactionSynchronizationManager.currentTransactionReadOnly}.
 * Without the lazy proxy,
 * {@link RoutingDataSource#determineCurrentLookupKey()} is always invoked
 * while readOnly is still {@code false}, defeating routing entirely. The
 * proxy defers the physical-connection acquisition until the first JDBC
 * statement, by which point the readOnly flag is set correctly.
 *
 * <h2>RLS continuity</h2>
 * <p>{@code RlsTenantContextAspect} calls
 * {@code DataSourceUtils.getConnection(dataSource)} inside the active
 * transaction. {@code DataSourceUtils} returns the connection Spring has
 * already bound to the current transaction (via the lazy proxy → routing
 * → chosen pool), so {@code set_config('app.tenant_id', ?, true)} fires
 * on the same physical connection the query will use. Routing is
 * transparent to the RLS aspect.
 *
 * <h2>AI module's readOnlyJdbcTemplate</h2>
 * <p>{@code modules/ai/.../ReadOnlyDataSourceConfig} registers an
 * independent {@code readOnlyJdbcTemplate} bean for the AI module's raw
 * analytics queries. That bean still exists and is untouched — it operates
 * outside the JPA tx routing path declared here.
 *
 * <h2>Dev/local fallback</h2>
 * <p>When {@code RO_DB_HOST} is unset, {@code readonly-datasource.jdbc-url}
 * resolves to the same host as {@code spring.datasource.url} (see
 * {@code application.yaml}). Both routing keys then point to the same
 * physical DB; routing still runs, behaviour is identical to pre-D.7 for
 * write paths, only a second Hikari pool shows up in metrics. The real
 * read offload arrives when a CNPG read replica is provisioned in
 * {@code lumie-infra} (separate ASK-FIRST work).
 */
@Configuration
public class RoutingDataSourceConfig {

    // --- Primary (read-write) DataSource ---------------------------------

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource primaryHikariDataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        HikariDataSource ds = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.setPoolName("PrimaryHikariPool");
        return ds;
    }

    // --- Read-only DataSource -------------------------------------------

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
        ds.setPoolName("RoutingReadOnlyHikariPool");
        return ds;
    }

    // --- Routing + lazy proxy -------------------------------------------

    @Bean
    public RoutingDataSource routingDataSource(
            @Qualifier("primaryHikariDataSource") DataSource primary,
            @Qualifier("readOnlyHikariDataSource") DataSource readOnly) {
        RoutingDataSource routing = new RoutingDataSource();
        routing.setTargetDataSources(Map.of(
                RoutingDataSource.KEY_PRIMARY, primary,
                RoutingDataSource.KEY_READONLY, readOnly
        ));
        routing.setDefaultTargetDataSource(primary);
        return routing;
    }

    /**
     * The {@code @Primary DataSource} the rest of the application sees
     * (Hibernate, JdbcTemplate, anything injecting {@code DataSource}).
     *
     * <p>Wraps the routing DS so connection acquisition is deferred until
     * after Spring has set the transaction's readOnly flag — see the
     * "Why LazyConnectionDataSourceProxy" section in the class Javadoc.
     */
    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("routingDataSource") RoutingDataSource routing) {
        return new LazyConnectionDataSourceProxy(routing);
    }

    // --- JPA stack (explicit; no autoconfig dependency) ------------------

    /**
     * {@code @Primary EntityManagerFactory} backed by the routing+lazy
     * DataSource. The bean MUST be named {@code entityManagerFactory} —
     * Spring Data JPA's {@code @EnableJpaRepositories} (see
     * {@link JpaConfig}) wires repository {@code EntityManager} proxies to
     * this name by default, and the internal
     * {@code jpaSharedEM_entityManagerFactory} alias resolves through it.
     *
     * <p>The injected {@link EntityManagerFactoryBuilder} is supplied by
     * Spring Boot's {@code JpaBaseConfiguration} and carries the configured
     * {@link org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter}
     * plus every property declared under {@code spring.jpa.*} in
     * {@code application.yaml} — so this explicit EMF inherits the same
     * Hibernate config (ddl-auto, batch_size, format_sql, etc.) the
     * autoconfigured EMF would have used.
     */
    @Bean(name = "entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.lumie")
                .persistenceUnit("default")
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
