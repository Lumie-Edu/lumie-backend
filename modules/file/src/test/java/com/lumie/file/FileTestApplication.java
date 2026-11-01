package com.lumie.file;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import java.time.Instant;
import java.util.Optional;

/** Test-only application anchor for module persistence slices. */
@SpringBootApplication
@EnableJpaAuditing(dateTimeProviderRef = "fileTestDateTimeProvider")
public class FileTestApplication {
    @Bean
    DateTimeProvider fileTestDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }
}
