package com.lumie.tenant.adapter.in.internal;

import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.application.service.TenantQueryService;
import com.lumie.tenant.application.service.TenantRegistrationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TenantServiceAdapterRegistrationTest {

    @Mock private TenantQueryService tenantQueryService;
    @Mock private TenantRegistrationService tenantRegistrationService;
    @Mock private TenantPersistencePort tenantPersistencePort;
    @InjectMocks private TenantServiceAdapter adapter;

    @Test
    void createTenantPassesSelectedSubjectsAndReturnsRegistrationResult() {
        List<String> subjects = List.of("KOREAN", "MATH");
        given(tenantRegistrationService.registerTenant("Academy", subjects))
                .willReturn(new TenantRegistrationService.TenantRegistrationResult(
                        true, "created", 22L, "academy-a"));

        var result = adapter.createTenant("Academy", subjects);

        assertThat(result.success()).isTrue();
        assertThat(result.tenantId()).isEqualTo(22L);
        assertThat(result.tenantSlug()).isEqualTo("academy-a");
        verify(tenantRegistrationService).registerTenant("Academy", subjects);
    }

    @Test
    void createTenantConvertsRegistrationFailureToFailureResult() {
        given(tenantRegistrationService.registerTenant("Academy", List.of("ENGLISH")))
                .willThrow(new IllegalStateException("database unavailable"));

        var result = adapter.createTenant("Academy", List.of("ENGLISH"));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("database unavailable");
        assertThat(result.tenantId()).isNull();
    }
}
