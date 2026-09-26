package com.lumie.tenant.application.service;

import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostTenantResolverTest {

    @Mock
    private TenantPersistencePort tenants;

    private HostTenantResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new HostTenantResolver(tenants);
        ReflectionTestUtils.setField(resolver, "baseDomain", "lumie-edu.com");
        ReflectionTestUtils.setField(resolver, "rootHosts", "lumie-edu.com,localhost");
        ReflectionTestUtils.setField(resolver, "customDomainMap", "joossameng.com=inst-joossameng");
        ReflectionTestUtils.setField(resolver, "loginThemeMap", "inst-joossameng=ocean");
    }

    @Test
    void resolvesBasicHostThroughImmutableInstNamespace() {
        Tenant tenant = Tenant.create("inst-joossameng", "Joossameng", null);
        when(tenants.findBySlug("inst-joossameng")).thenReturn(Optional.of(tenant));

        assertThat(resolver.resolve("joossameng.lumie-edu.com:443"))
                .isPresent()
                .get()
                .extracting(result -> result.tenant().getSlugValue())
                .isEqualTo("inst-joossameng");

        verify(tenants).findBySlug("inst-joossameng");
    }

    @Test
    void fallsBackToTheLegacySlugWhenInstNamespaceIsNotYetMigrated() {
        Tenant tenant = Tenant.create("joossameng", "Joossameng", null);
        when(tenants.findBySlug("inst-joossameng")).thenReturn(Optional.empty());
        when(tenants.findBySlug("joossameng")).thenReturn(Optional.of(tenant));

        assertThat(resolver.resolve("joossameng.lumie-edu.com"))
                .isPresent()
                .get()
                .extracting(result -> result.tenant().getSlugValue())
                .isEqualTo("joossameng");

        verify(tenants).findBySlug("inst-joossameng");
        verify(tenants).findBySlug("joossameng");
    }

    @Test
    void resolvesInfraOwnedCustomHost() {
        Tenant tenant = Tenant.create("inst-joossameng", "Joossameng", null);
        when(tenants.findBySlug("inst-joossameng")).thenReturn(Optional.of(tenant));

        assertThat(resolver.resolve("JOOSSAMENG.COM"))
                .isPresent()
                .get()
                .extracting(HostTenantResolver.ResolvedTenant::customDomain)
                .isEqualTo("joossameng.com");
        assertThat(resolver.loginThemeFor("inst-joossameng")).isEqualTo("ocean");
    }

    @Test
    void leavesOnlyConfiguredRootHostsUnresolved() {
        assertThat(resolver.resolve("lumie-edu.com")).isEmpty();
        assertThat(resolver.isRootHost("lumie-edu.com")).isTrue();
        assertThat(resolver.resolve("unknown.example")).isEmpty();
        assertThat(resolver.isRootHost("unknown.example")).isFalse();

        verifyNoInteractions(tenants);
    }
}
