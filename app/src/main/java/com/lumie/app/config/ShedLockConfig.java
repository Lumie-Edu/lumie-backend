package com.lumie.app.config;

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Distributed lock for {@code @Scheduled} methods so multi-pod deployments
 * don't double-execute cron jobs.
 *
 * <p>The {@code shedlock} table is pinned to the {@code public} schema (DDL in
 * Flyway V14) — same rationale as the Modulith outbox: schema-per-tenant
 * Hibernate routing must not redirect cross-tenant infrastructure tables.
 *
 * <p>{@code defaultLockAtMostFor} is the safety net for jobs that crash without
 * releasing the lock; per-method values override it on long-running schedulers.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .withTableName("public.shedlock")
                        .usingDbTime()
                        .build()
        );
    }
}
