package com.lumie.tenant.application.service;

import com.lumie.tenant.api.TenantStatus;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TenantRegistrationServiceTest {

    @Mock private TenantPersistencePort tenants;
    @InjectMocks private TenantRegistrationService service;

    @Test
    void registersTenantWithSelectedSubjectsAndActivatesIt() {
        given(tenants.save(any(Tenant.class))).willAnswer(invocation -> invocation.getArgument(0));

        TenantRegistrationService.TenantRegistrationResult result =
                service.registerTenant("Academy", List.of("KOREAN", "SCIENCE"));

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenants, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(tenant -> {
            assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
            assertThat(tenant.getSubjects()).containsExactly("KOREAN", "SCIENCE");
        });
        assertThat(result.success()).isTrue();
        assertThat(result.tenantSlug()).matches("inst-[0-9a-f]{8}");
    }

    @Test
    void legacyRegistrationOverloadUsesNoSubjects() {
        given(tenants.save(any(Tenant.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.registerTenant("Academy");

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenants, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(tenant -> assertThat(tenant.getSubjects()).isEmpty());
    }
}
