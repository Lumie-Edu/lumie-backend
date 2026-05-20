package com.lumie.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Instant;
import java.util.Optional;

/**
 * Spring Data JPA Auditing wiring for {@code BaseEntity}'s
 * {@code @CreatedDate} / {@code @LastModifiedDate} fields.
 *
 * <p>The custom {@link DateTimeProvider} returns {@link Instant#now()} directly
 * to preserve UTC parity with the previous manual {@code Instant.now()} hook.
 * The framework default ({@code CurrentDateTimeProvider}) returns
 * {@code LocalDateTime.now()} in the JVM default zone, which would silently
 * apply a zone shift when assigned to an {@code Instant} column.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingInstantProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider auditingInstantProvider() {
        return () -> Optional.of(Instant.now());
    }
}
