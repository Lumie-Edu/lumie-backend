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

    public static void setTenantSchema(JdbcTemplate jdbcTemplate, String tenantSlug) {
        String schema = "tenant_" + tenantSlug.replace("-", "_");
        jdbcTemplate.execute("SET search_path TO " + schema + ", public");
    }
}
