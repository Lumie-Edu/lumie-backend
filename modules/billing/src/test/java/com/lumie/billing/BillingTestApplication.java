package com.lumie.billing;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application for module-level slice tests (@DataJpaTest, etc.).
 *
 * <p>This class provides the required {@code @SpringBootConfiguration} anchor so that
 * Spring slice test annotations (which scan upward for a boot configuration class) can
 * locate one within the module. It is intentionally placed at the root test package so it
 * is invisible to production builds. The slim {@code @SpringBootApplication} includes no
 * component scan of production packages — slice test annotations manage their own context.
 */
@SpringBootApplication
public class BillingTestApplication {
}
