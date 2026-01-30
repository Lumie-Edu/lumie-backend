package com.lumie.tenant.application.service;

import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantQueryServiceTest {

    @Mock
    private TenantPersistencePort tenantPersistencePort;

    @InjectMocks
    private TenantQueryService tenantQueryService;

    @Test
    @DisplayName("slug로 테넌트 조회 성공")
    void getTenantBySlug() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", "테스트 학원", "owner@test.com");
        when(tenantPersistencePort.findBySlug("test-academy")).thenReturn(Optional.of(tenant));

        TenantResponse response = tenantQueryService.getTenantBySlug("test-academy");

        assertThat(response.slug()).isEqualTo("test-academy");
        assertThat(response.name()).isEqualTo("Test Academy");
    }

    @Test
    @DisplayName("존재하지 않는 테넌트 조회 시 예외")
    void getTenantNotFound() {
        when(tenantPersistencePort.findBySlug("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantQueryService.getTenantBySlug("non-existent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("모든 테넌트 조회")
    void getAllTenants() {
        Tenant tenant1 = Tenant.create("academy-1", "Academy 1", null, null);
        Tenant tenant2 = Tenant.create("academy-2", "Academy 2", null, null);
        when(tenantPersistencePort.findAll()).thenReturn(List.of(tenant1, tenant2));

        List<TenantResponse> responses = tenantQueryService.getAllTenants();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).slug()).isEqualTo("academy-1");
        assertThat(responses.get(1).slug()).isEqualTo("academy-2");
    }

    @Test
    @DisplayName("테넌트 유효성 검증 - 활성 상태")
    void validateActiveTenant() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);
        tenant.startProvisioning();
        tenant.activate();
        when(tenantPersistencePort.findBySlug("test-academy")).thenReturn(Optional.of(tenant));

        boolean isValid = tenantQueryService.isValidAndActive("test-academy");

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("테넌트 유효성 검증 - 비활성 상태")
    void validateInactiveTenant() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);
        when(tenantPersistencePort.findBySlug("test-academy")).thenReturn(Optional.of(tenant));

        boolean isValid = tenantQueryService.isValidAndActive("test-academy");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("테넌트 ID 조회 - 존재하는 경우")
    void getTenantIdBySlugExists() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);
        ReflectionTestUtils.setField(tenant, "id", 1L);
        when(tenantPersistencePort.findBySlug("test-academy")).thenReturn(Optional.of(tenant));

        Optional<Long> tenantId = tenantQueryService.getTenantIdBySlug("test-academy");

        assertThat(tenantId).isPresent();
        assertThat(tenantId.get()).isEqualTo(1L);
    }

    @Test
    @DisplayName("테넌트 ID 조회 - 존재하지 않는 경우")
    void getTenantIdBySlugNotExists() {
        when(tenantPersistencePort.findBySlug("non-existent")).thenReturn(Optional.empty());

        Optional<Long> tenantId = tenantQueryService.getTenantIdBySlug("non-existent");

        assertThat(tenantId).isEmpty();
    }
}
