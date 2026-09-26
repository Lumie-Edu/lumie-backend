package com.lumie.tenant.adapter.in.web;

import com.lumie.common.tenant.RequestContextFilter;
import com.lumie.tenant.application.service.HostTenantResolver;
import com.lumie.tenant.domain.entity.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostTenantFilterTest {

    @Mock
    private HostTenantResolver resolver;

    @AfterEach
    void clearContext() {
        com.lumie.common.tenant.TenantContextHolder.clear();
    }

    @Test
    void bindsActiveTenantFromHostBeforeAuthentication() throws Exception {
        Tenant tenant = activeTenant("inst-academy", 17L);
        when(resolver.resolve("academy.lumie-edu.com"))
                .thenReturn(Optional.of(new HostTenantResolver.ResolvedTenant(tenant, null)));

        MockHttpServletRequest request = request("academy.lumie-edu.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new HostTenantFilter(resolver).doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            assertThat(request.getAttribute(RequestContextFilter.HOST_TENANT_SLUG_ATTRIBUTE))
                    .isEqualTo("inst-academy");
            assertThat(com.lumie.common.tenant.TenantContextHolder.getTenant()).isEqualTo("inst-academy");
        });

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsUnknownInactiveAndUnavailableTenantHosts() throws Exception {
        MockHttpServletResponse unknownResponse = runWith("unknown.example", Optional.empty());
        assertThat(unknownResponse.getStatus()).isEqualTo(404);

        Tenant inactive = Tenant.create("inst-inactive", "Inactive", null);
        MockHttpServletResponse inactiveResponse = runWith("inactive.lumie-edu.com",
                Optional.of(new HostTenantResolver.ResolvedTenant(inactive, null)));
        assertThat(inactiveResponse.getStatus()).isEqualTo(403);

        when(resolver.resolve("failure.lumie-edu.com"))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));
        MockHttpServletResponse failureResponse = new MockHttpServletResponse();
        new HostTenantFilter(resolver).doFilter(request("failure.lumie-edu.com"), failureResponse,
                (ignoredRequest, ignoredResponse) -> { });
        assertThat(failureResponse.getStatus()).isEqualTo(503);
    }

    private MockHttpServletResponse runWith(String host, Optional<HostTenantResolver.ResolvedTenant> resolved) throws Exception {
        when(resolver.resolve(host)).thenReturn(resolved);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new HostTenantFilter(resolver).doFilter(request(host), response, (ignoredRequest, ignoredResponse) -> { });
        return response;
    }

    private MockHttpServletRequest request(String host) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/tenants/public/current-branding");
        request.addHeader("Host", host);
        return request;
    }

    private Tenant activeTenant(String slug, long id) {
        Tenant tenant = Tenant.create(slug, "Academy", null);
        tenant.startProvisioning();
        tenant.activate();
        ReflectionTestUtils.setField(tenant, "id", id);
        return tenant;
    }
}
