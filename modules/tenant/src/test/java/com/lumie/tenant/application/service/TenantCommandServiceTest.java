package com.lumie.tenant.application.service;

import com.lumie.common.exception.DuplicateResourceException;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.tenant.application.dto.request.CompleteTenantOnboardingRequest;
import com.lumie.tenant.application.dto.request.CreateTenantRequest;
import com.lumie.tenant.application.dto.request.UpdateTenantRequest;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.api.TenantCreatedEvent;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.api.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantCommandServiceTest {

    @Mock
    private TenantPersistencePort tenantPersistencePort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TenantCommandService tenantCommandService;

    private CreateTenantRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = CreateTenantRequest.builder()
                .slug("test-academy")
                .name("Test Academy")
                .displayName("테스트 학원")
                .build();
    }

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("테넌트 생성 성공")
    void createTenantSuccess() {
        when(tenantPersistencePort.existsBySlug("test-academy")).thenReturn(false);
        when(tenantPersistencePort.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantResponse response = tenantCommandService.createTenant(createRequest);

        assertThat(response.slug()).isEqualTo("test-academy");
        assertThat(response.name()).isEqualTo("Test Academy");
        assertThat(response.status()).isEqualTo(TenantStatus.ACTIVE);

        verify(eventPublisher).publishEvent(any(TenantCreatedEvent.class));
    }

    @Test
    @DisplayName("중복 slug로 테넌트 생성 시 예외")
    void createTenantDuplicateSlug() {
        when(tenantPersistencePort.existsBySlug("test-academy")).thenReturn(true);

        assertThatThrownBy(() -> tenantCommandService.createTenant(createRequest))
                .isInstanceOf(DuplicateResourceException.class);

        verify(tenantPersistencePort, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("테넌트 정보 업데이트")
    void updateTenant() {
        Tenant existingTenant = Tenant.create("test-academy", "Test Academy", null);
        when(tenantPersistencePort.findBySlug("test-academy")).thenReturn(Optional.of(existingTenant));
        when(tenantPersistencePort.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTenantRequest updateRequest = UpdateTenantRequest.builder()
                .name("Updated Academy")
                .displayName("업데이트된 학원")
                .hiddenSidebarItemIds(List.of("attendance", "reviews"))
                .build();

        TenantResponse response = tenantCommandService.updateTenant("test-academy", updateRequest);

        assertThat(response.name()).isEqualTo("Updated Academy");
        assertThat(response.displayName()).isEqualTo("업데이트된 학원");
        assertThat(response.hiddenSidebarItemIds()).containsExactly("attendance", "reviews");
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
    @DisplayName("테넌트 온보딩 완료")
    void completeOnboarding() {
        TenantContextHolder.setTenant("test-academy");
        UserContextHolder.setUserRole(Role.OWNER);
        Tenant existingTenant = Tenant.create("test-academy", "Test Academy", null);
        when(tenantPersistencePort.findBySlug("test-academy")).thenReturn(Optional.of(existingTenant));
        when(tenantPersistencePort.existsByCustomId("academy")).thenReturn(false);
        when(tenantPersistencePort.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompleteTenantOnboardingRequest request = CompleteTenantOnboardingRequest.builder()
                .customId("academy")
                .phone("02-1234-5678")
                .address("서울시 강남구")
                .hiddenSidebarItemIds(List.of("attendance", "reviews"))
                .build();

        TenantResponse response = tenantCommandService.completeOnboarding("test-academy", request);

        assertThat(response.customId()).isEqualTo("academy");
        assertThat(response.phone()).isEqualTo("02-1234-5678");
        assertThat(response.address()).isEqualTo("서울시 강남구");
        assertThat(response.hiddenSidebarItemIds()).containsExactly("attendance", "reviews");
        assertThat(response.onboardingCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("완료된 온보딩 재시도 시 정보 변경 없이 예외")
    void completeOnboardingAlreadyCompleted() {
        TenantContextHolder.setTenant("test-academy");
        UserContextHolder.setUserRole(Role.OWNER);
        Tenant existingTenant = Tenant.create("test-academy", "Test Academy", null);
        existingTenant.completeOnboarding(java.time.Instant.now());
        when(tenantPersistencePort.findBySlug("test-academy")).thenReturn(Optional.of(existingTenant));

        CompleteTenantOnboardingRequest request = CompleteTenantOnboardingRequest.builder()
                .phone("02-9999-9999")
                .build();

        assertThatThrownBy(() -> tenantCommandService.completeOnboarding("test-academy", request))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);

        verify(tenantPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("원장이 아닌 사용자는 온보딩을 완료할 수 없음")
    void completeOnboardingRequiresOwner() {
        TenantContextHolder.setTenant("test-academy");
        UserContextHolder.setUserRole(Role.MANAGER);

        CompleteTenantOnboardingRequest request = CompleteTenantOnboardingRequest.builder()
                .phone("02-1234-5678")
                .build();

        assertThatThrownBy(() -> tenantCommandService.completeOnboarding("test-academy", request))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);

        verify(tenantPersistencePort, never()).findBySlug(any());
        verify(tenantPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("테넌트 삭제 (소프트 삭제)")
    void deleteTenant() {
        Tenant existingTenant = Tenant.create("test-academy", "Test Academy", null);
        when(tenantPersistencePort.findBySlug("test-academy")).thenReturn(Optional.of(existingTenant));
        when(tenantPersistencePort.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tenantCommandService.deleteTenant("test-academy");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantPersistencePort).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getStatus()).isEqualTo(TenantStatus.DELETED);
    }
}
