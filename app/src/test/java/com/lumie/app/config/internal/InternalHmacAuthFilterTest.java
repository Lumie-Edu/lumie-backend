package com.lumie.app.config.internal;

import com.lumie.tenant.api.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InternalHmacAuthFilterTest {

    private final ExposedInternalHmacAuthFilter filter = new ExposedInternalHmacAuthFilter(
            new InternalAuthProperties("test-secret-that-is-long-enough-for-hmac"),
            mock(TenantService.class));

    @Test
    void protectsEveryInternalRoute() {
        assertThat(filter.shouldSkip("/internal/v1/tenant-context")).isFalse();
        assertThat(filter.shouldSkip("/internal/v1/jobs/complete")).isFalse();
        assertThat(filter.shouldSkip("/v1/login")).isTrue();
    }

    private static final class ExposedInternalHmacAuthFilter extends InternalHmacAuthFilter {
        private ExposedInternalHmacAuthFilter(InternalAuthProperties properties, TenantService tenantService) {
            super(properties, tenantService);
        }

        private boolean shouldSkip(String path) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            return shouldNotFilter(request);
        }
    }
}
