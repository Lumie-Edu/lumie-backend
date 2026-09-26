package com.lumie.tenant.adapter.in.web;

import com.lumie.common.tenant.RequestContextFilter;
import com.lumie.tenant.application.service.HostTenantResolver;
import com.lumie.tenant.domain.entity.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRootControllerTest {

    private final TenantRootController controller = new TenantRootController();

    @Test
    void redirectsBasicHostRootToLogin() {
        MockHttpServletRequest request = request(new HostTenantResolver.ResolvedTenant(activeTenant(), null));

        assertThat(controller.redirectToTenantLogin(request).getStatusCode().value()).isEqualTo(307);
        assertThat(controller.redirectToTenantLogin(request).getHeaders().getLocation()).hasToString("/login");
    }

    @Test
    void preservesTraefikReplacedPathAndQueryForCustomApexRedirect() {
        MockHttpServletRequest request = request(new HostTenantResolver.ResolvedTenant(activeTenant(), "joossameng.com"));
        request.addHeader("X-Replaced-Path", "/dashboard");
        request.setQueryString("from=basic");

        assertThat(controller.redirectToTenantLogin(request).getHeaders().getLocation())
                .hasToString("https://joossameng.com/dashboard?from=basic");
    }

    @Test
    void rejectsUnsafeReplacedPathBeforeBuildingCustomApexRedirect() {
        MockHttpServletRequest request = request(new HostTenantResolver.ResolvedTenant(activeTenant(), "joossameng.com"));
        request.addHeader("X-Replaced-Path", "//attacker.example");

        assertThat(controller.redirectToTenantLogin(request).getHeaders().getLocation())
                .hasToString("https://joossameng.com/");
    }

    private MockHttpServletRequest request(HostTenantResolver.ResolvedTenant tenant) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tenant-root");
        request.setAttribute(RequestContextFilter.HOST_TENANT_RESOLUTION_ATTRIBUTE, tenant);
        return request;
    }

    private Tenant activeTenant() {
        Tenant tenant = Tenant.create("inst-academy", "Academy", null);
        tenant.startProvisioning();
        tenant.activate();
        ReflectionTestUtils.setField(tenant, "id", 17L);
        return tenant;
    }
}
