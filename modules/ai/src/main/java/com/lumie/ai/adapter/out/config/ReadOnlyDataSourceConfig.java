package com.lumie.ai.adapter.out.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ReadOnlyDataSourceConfig {

    @Bean
    public JdbcTemplate readOnlyJdbcTemplate(
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
