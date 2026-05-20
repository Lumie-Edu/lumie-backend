package com.lumie.app.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
 * <p>{@code @FlywayDataSource} pins the primary {@link HikariDataSource} so
 * {@code FlywayAutoConfiguration} introspects a raw pool (not the lazy
 * proxy) when {@code spring.flyway.user/password} differ from runtime
 * credentials. Without this, {@code DataSourceBuilder.derivedFrom(...)}
 * fails with {@code "Unable to find suitable method for url"}.
 */
@Configuration
public class RoutingDataSourceConfig {

    @Bean
    @ConfigurationProperties("app.datasource.primary")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @FlywayDataSource
    @ConfigurationProperties("app.datasource.primary.hikari")
    public HikariDataSource primaryDataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
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
