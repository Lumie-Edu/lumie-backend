package com.lumie.ai.infrastructure.config;

import com.lumie.common.tenant.TenantContextHolder;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class ReadOnlyDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.readonly")
    public DataSource readOnlyDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public JdbcTemplate readOnlyJdbcTemplate(@Qualifier("readOnlyDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    public static void setTenantSchema(JdbcTemplate jdbcTemplate, String tenantSlug) {
        String schema = "tenant_" + tenantSlug.replace("-", "_");
        jdbcTemplate.execute("SET search_path TO " + schema + ", public");
    }
}
