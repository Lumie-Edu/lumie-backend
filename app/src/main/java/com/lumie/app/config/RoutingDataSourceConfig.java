package com.lumie.app.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Read/write split via {@link RoutingDataSource}.
 *
 * <p>Two HikariCP pools are declared from {@code app.datasource.primary} and
 * {@code app.datasource.readonly} (non-{@code spring.datasource} prefix so
 * {@code DataSourceAutoConfiguration} skips entirely) and exposed as a
 * single {@code @Primary} {@link LazyConnectionDataSourceProxy} — the
 * canonical Spring Boot multi-datasource pattern.
 *
 * <p>{@code @FlywayDataSource} is on a dedicated migrator-credential
 * {@link DataSource} (not the runtime {@code lumie_app} pool). Pinning it
 * to {@code primaryDataSource} would force Flyway to connect with runtime
 * credentials (a {@code NOSUPERUSER NOBYPASSRLS} role that does not own
 * the platform tables) and break any DDL that requires ownership — e.g.
 * {@code ALTER TABLE ... RENAME}. The migrator DataSource shares the
 * primary URL/driver but overrides user/password from
 * {@code spring.flyway.user/password}, so migrations run as the
 * privileged role per HARD_RULES_CROSS while runtime traffic stays on
 * {@code lumie_app}.
 */
@Configuration
public class RoutingDataSourceConfig {

    @Bean
    @ConfigurationProperties("app.datasource.primary")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.datasource.primary.hikari")
    public HikariDataSource primaryDataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * Migrator-credential DataSource for Flyway. Inherits URL/driver from
     * the primary properties (same host/db/port as runtime) but binds
     * {@code spring.flyway.user/password} so DDL runs as the privileged
     * role per HARD_RULES_CROSS. Not pooled — Flyway opens at most one
     * connection at boot.
     */
    @Bean
    @FlywayDataSource
    public DataSource flywayMigratorDataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties primary,
            @Value("${spring.flyway.user}") String migratorUser,
            @Value("${spring.flyway.password}") String migratorPassword) {
        return DataSourceBuilder.create()
                .url(primary.determineUrl())
                .driverClassName(primary.determineDriverClassName())
                .username(migratorUser)
                .password(migratorPassword)
                .build();
    }

    @Bean
    @ConfigurationProperties("app.datasource.readonly")
    public DataSourceProperties readOnlyDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.datasource.readonly.hikari")
    public HikariDataSource readOnlyDataSource(
            @Qualifier("readOnlyDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    public DataSource dataSource(
            @Qualifier("primaryDataSource") DataSource primary,
            @Qualifier("readOnlyDataSource") DataSource readOnly) {
        RoutingDataSource routing = new RoutingDataSource();
        routing.setTargetDataSources(Map.of(
                RoutingDataSource.KEY_PRIMARY, primary,
                RoutingDataSource.KEY_READONLY, readOnly));
        routing.setDefaultTargetDataSource(primary);
        // Created as an internal — not a Spring bean — so Spring will not
        // call its InitializingBean hook for us. AbstractRoutingDataSource
        // resolves its targets lazily in afterPropertiesSet(); without
        // this call determineTargetDataSource() throws "DataSource router
        // not initialized" on the first JDBC use.
        routing.afterPropertiesSet();
        return new LazyConnectionDataSourceProxy(routing);
    }
}
