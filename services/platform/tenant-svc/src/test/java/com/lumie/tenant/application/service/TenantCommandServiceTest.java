package com.lumie.tenant.application.service;

import com.lumie.common.exception.DuplicateResourceException;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.messaging.event.TenantCreatedEvent;
import com.lumie.tenant.application.dto.request.CreateTenantRequest;
import com.lumie.tenant.application.dto.request.UpdateTenantRequest;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.out.EventPublisherPort;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.domain.vo.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantCommandServiceTest {

    @Mock
    private TenantPersistencePort tenantPersistencePort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private TenantCommandService tenantCommandService;

    private CreateTenantRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = CreateTenantRequest.builder()
                .slug("test-academy")
                .name("Test Academy")
                .displayName("테스트 학원")
                .ownerEmail("owner@test.com")
                .build();
    }

    @Test
    @DisplayName("테넌트 생성 성공")
    void createTenantSuccess() {
        when(tenantPersistencePort.existsBySlug("test-academy")).thenReturn(false);
        when(tenantPersistencePort.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantResponse response = tenantCommandService.createTenant(createRequest);

        assertThat(response.slug()).isEqualTo("test-academy");
        assertThat(response.name()).isEqualTo("Test Academy");
        assertThat(response.status()).isEqualTo(TenantStatus.PENDING);

        ArgumentCaptor<TenantCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TenantCreatedEvent.class);
        verify(eventPublisherPort).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getSlug()).isEqualTo("test-academy");
    }

    @Test
    @DisplayName("중복 slug로 테넌트 생성 시 예외")
    void createTenantDuplicateSlug() {
        when(tenantPersistencePort.existsBySlug("test-academy")).thenReturn(true);

        assertThatThrownBy(() -> tenantCommandService.createTenant(createRequest))
                .isInstanceOf(DuplicateResourceException.class);

        verify(tenantPersistencePort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    @DisplayName("테넌트 정보 업데이트")
    void updateTenant() {
        Tenant existingTenant = Tenant.create("test-academy", "Test Academy", null, null);
        when(tenantPersistencePort.findBySlug("test-academy")).thenReturn(Optional.of(existingTenant));
        when(tenantPersistencePort.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTenantRequest updateRequest = UpdateTenantRequest.builder()
                .name("Updated Academy")
                .displayName("업데이트된 학원")
                .build();

        TenantResponse response = tenantCommandService.updateTenant("test-academy", updateRequest);

        assertThat(response.name()).isEqualTo("Updated Academy");
        assertThat(response.displayName()).isEqualTo("업데이트된 학원");
    }

    @Test
    @DisplayName("존재하지 않는 테넌트 업데이트 시 예외")
    void updateTenantNotFound() {
        when(tenantPersistencePort.findBySlug("non-existent")).thenReturn(Optional.empty());

        UpdateTenantRequest updateRequest = UpdateTenantRequest.builder()
                .name("Updated Academy")
                .build();

        assertThatThrownBy(() -> tenantCommandService.updateTenant("non-existent", updateRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("테넌트 삭제 (소프트 삭제)")
    void deleteTenant() {
        Tenant existingTenant = Tenant.create("test-academy", "Test Academy", null, null);
        when(tenantPersistencePort.findBySlug("test-academy")).thenReturn(Optional.of(existingTenant));
        when(tenantPersistencePort.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tenantCommandService.deleteTenant("test-academy");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantPersistencePort).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getStatus()).isEqualTo(TenantStatus.DELETED);
    }
}
