package com.lumie.ai.adapter.out.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Raw {@link JdbcTemplate} on a dedicated read-only HikariCP pool, used by
 * the AI module's analytics queries that intentionally bypass JPA
 * (schema discovery + NL→SQL agent).
 *
 * <p>This pool is independent of the application-wide read/write routing
 * declared in {@code app/.../RoutingDataSourceConfig}. The shared routing
 * handles the JPA path automatically for any {@code @Transactional(readOnly
 * = true)} service; this bean exists for the few callers that need a raw
 * {@code JdbcTemplate} that explicitly never holds a write connection,
 * picked up via {@code @Qualifier("readOnlyJdbcTemplate")}.
 *
 * <p>Both this pool and the routing config read the same
 * {@code app.datasource.readonly.*} block in {@code application.yaml},
 * so they target the same physical replica when one is available, and the
 * same primary host in dev where no replica exists.
 */
@Configuration
public class ReadOnlyDataSourceConfig {

    @Bean
    public JdbcTemplate readOnlyJdbcTemplate(
            @Value("${app.datasource.readonly.url}") String jdbcUrl,
            @Value("${app.datasource.readonly.username}") String username,
            @Value("${app.datasource.readonly.password}") String password,
            @Value("${app.datasource.readonly.driver-class-name}") String driverClassName,
            @Value("${app.datasource.readonly.hikari.maximum-pool-size:5}") int maxPoolSize,
            @Value("${app.datasource.readonly.hikari.minimum-idle:2}") int minIdle,
            @Value("${app.datasource.readonly.hikari.connection-timeout:10000}") long connectionTimeout) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        ds.setMaximumPoolSize(maxPoolSize);
        ds.setMinimumIdle(minIdle);
        ds.setConnectionTimeout(connectionTimeout);
        ds.setReadOnly(true);
        ds.setPoolName("AiReadOnlyHikariPool");
        return new JdbcTemplate(ds);
    }

    /**
     * Bind the RLS tenant GUC on the read-only connection. Post-RLS
     * (2026-05-12) the readonly path no longer routes by schema —
     * Postgres RLS policies filter rows by {@code current_setting(
     * 'app.tenant_id')}, so callers SET that GUC instead of switching
     * search_path.
     *
     * <p>This helper uses {@code SET LOCAL}, which means it only persists
     * for the current JDBC transaction. The two callers
     * ({@code SchemaDiscoveryService}, {@code GeneralQueryTool}) wrap
     * their queries in {@code @Transactional(readOnly=true)} so the
     * setting carries into Hibernate's read path.
     */
    public static void setTenantSchema(JdbcTemplate jdbcTemplate, Long tenantId) {
        jdbcTemplate.update("SELECT set_config('app.tenant_id', ?, true)",
                String.valueOf(tenantId));
    }
}
