package com.lumie.common.test;

import com.lumie.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that populates {@link TenantContextHolder} before each test
 * and clears it after, eliminating the {@code @BeforeEach}/{@code @AfterEach}
 * boilerplate that was copy-pasted across service tests.
 *
 * <p>Default values — override by subclassing or via constructor:
 * <ul>
 *   <li>tenant slug: {@code "test-tenant"}
 *   <li>tenant id: {@code 1L}
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   &#64;ExtendWith({MockitoExtension.class, TenantContextExtension.class})
 *   class MyServiceTest { ... }
 * </pre>
 *
 * <p>Custom slug / id:
 * <pre>
 *   &#64;RegisterExtension
 *   TenantContextExtension tenant = new TenantContextExtension("acme", 7L);
 * </pre>
 */
public class TenantContextExtension implements BeforeEachCallback, AfterEachCallback {

    private final String tenantSlug;
    private final Long tenantId;

    /** Constructs with default test-tenant slug and id=1. */
    public TenantContextExtension() {
        this("test-tenant", 1L);
    }

    public TenantContextExtension(String tenantSlug, Long tenantId) {
        this.tenantSlug = tenantSlug;
        this.tenantId = tenantId;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        TenantContextHolder.setTenant(tenantSlug);
        TenantContextHolder.setTenantId(tenantId);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        TenantContextHolder.clear();
    }
}
