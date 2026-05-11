package com.lumie.tenant.adapter.out.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom {@link FlywayMigrationStrategy} kept as a hook for any future
 * non-default behavior. The previous version called {@code flyway.repair()}
 * before every migrate — that silently mutated `flyway_schema_history`
 * checksums when a dev edited an already-applied migration, masking real
 * drift in prod.
 *
 * <p>If repair is genuinely needed (e.g. after deleting a failed migration
 * row), invoke {@code FlywayTenantMigrationService.repairSchema(...)}
 * explicitly via an operator command or one-off Job. Do NOT add it back to
 * the boot path.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return Flyway::migrate;
    }
}
