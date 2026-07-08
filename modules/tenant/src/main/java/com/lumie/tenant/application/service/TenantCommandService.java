package com.lumie.tenant.application.service;

import com.lumie.common.exception.DuplicateResourceException;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.common.cache.CacheConfig;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.tenant.application.dto.request.CompleteTenantOnboardingRequest;
import com.lumie.tenant.application.dto.request.CreateTenantRequest;
import com.lumie.tenant.application.dto.request.UpdateTenantRequest;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.api.TenantCreatedEvent;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.domain.exception.TenantErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Tenant lifecycle commands. Post-RLS (2026-05-12), tenant onboarding is
 * a single INSERT into {@code public.tenants} — no per-tenant schema
 * creation or Flyway migration. The {@code PENDING → PROVISIONING → ACTIVE}
 * state machine is preserved as a billing/payment hook surface; the
 * provisioning phase is now effectively instantaneous.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TenantCommandService {

    private final TenantPersistencePort tenantPersistencePort;
    private final ApplicationEventPublisher eventPublisher;

    public TenantResponse createTenant(CreateTenantRequest request) {
        log.info("Creating tenant with slug: {}", request.slug());

        if (tenantPersistencePort.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Tenant", request.slug());
        }

        Tenant tenant = Tenant.create(
                request.slug(),
                request.name(),
                request.displayName()
        );

        Tenant savedTenant = tenantPersistencePort.save(tenant);
        log.info("Tenant created with ID: {}, slug: {}", savedTenant.getId(), savedTenant.getSlugValue());

        savedTenant.startProvisioning();
        savedTenant.activate();
        tenantPersistencePort.save(savedTenant);

        // Free-subscription provisioning runs as an AFTER_COMMIT listener on this
        // event in billing. Breaks the previous tenant↔billing cycle
        // (CIRCULAR_MODULE_DEPS_AUDIT Resolution A, pair 3).
        eventPublisher.publishEvent(new TenantCreatedEvent(savedTenant.getId(), savedTenant.getSlugValue()));

        return TenantResponse.from(savedTenant);
    }

    @CacheEvict(value = CacheConfig.CACHE_TENANTS_BY_SLUG, key = "#slug")
    public TenantResponse updateTenant(String slug, UpdateTenantRequest request) {
        log.info("Updating tenant with slug: {}", slug);

        Tenant tenant = findTenantOrThrow(slug);
        tenant.updateInfo(request.name(), request.displayName());
        tenant.updateContact(request.phone());
        applyCustomIdUpdate(tenant, request.customId());
        tenant.updateHiddenSidebarItemIds(request.hiddenSidebarItemIds());
        Tenant updatedTenant = tenantPersistencePort.save(tenant);

        log.info("Tenant updated: {}", slug);
        return TenantResponse.from(updatedTenant);
    }

    @CacheEvict(value = CacheConfig.CACHE_TENANTS_BY_SLUG, key = "#slug")
    public TenantResponse completeOnboarding(String slug, CompleteTenantOnboardingRequest request) {
        log.info("Completing tenant onboarding with slug: {}", slug);

        AuthorizationGuard.requireOwner();
        String contextTenant = TenantContextHolder.getTenant();
        if (contextTenant == null || !slug.equals(contextTenant)) {
            throw new BusinessException(TenantErrorCode.TENANT_CONTEXT_MISMATCH,
                    "요청 학원과 인증 학원이 일치하지 않습니다.");
        }

        Tenant tenant = findTenantOrThrow(slug);
        if (tenant.getOnboardingCompletedAt() != null) {
            throw new BusinessException(TenantErrorCode.ONBOARDING_ALREADY_COMPLETED,
                    "이미 완료된 온보딩입니다.");
        }
        tenant.updateContact(request.phone());
        applyCustomIdUpdate(tenant, request.customId());
        tenant.updateHiddenSidebarItemIds(request.hiddenSidebarItemIds());
        tenant.completeOnboarding(Instant.now());
        Tenant updatedTenant = tenantPersistencePort.save(tenant);

        log.info("Tenant onboarding completed: {}", slug);
        return TenantResponse.from(updatedTenant);
    }

    private void applyCustomIdUpdate(Tenant tenant, String requested) {
        if (requested == null) {
            return;
        }
        String normalized = requested.isBlank() ? null : requested.trim().toLowerCase(java.util.Locale.ROOT);
        if (java.util.Objects.equals(normalized, tenant.getCustomId())) {
            return;
        }
        if (normalized != null && tenantPersistencePort.existsByCustomId(normalized)) {
            throw new DuplicateResourceException("CustomId", normalized);
        }
        tenant.updateCustomId(normalized);
    }

    public void deleteTenant(String slug) {
        log.info("Deleting tenant with slug: {}", slug);

        Tenant tenant = findTenantOrThrow(slug);
        tenant.markAsDeleted();
        tenantPersistencePort.save(tenant);

        log.info("Tenant marked as deleted: {}", slug);
    }

    public TenantResponse suspendTenant(String slug, String reason) {
        log.info("Suspending tenant: {} with reason: {}", slug, reason);

        Tenant tenant = findTenantOrThrow(slug);
        tenant.suspend();
        Tenant savedTenant = tenantPersistencePort.save(tenant);

        return TenantResponse.from(savedTenant);
    }

    public TenantResponse reactivateTenant(String slug) {
        log.info("Reactivating tenant: {}", slug);

        Tenant tenant = findTenantOrThrow(slug);
        tenant.reactivate();
        Tenant savedTenant = tenantPersistencePort.save(tenant);

        log.info("Tenant reactivated: {}", slug);
        return TenantResponse.from(savedTenant);
    }

    private Tenant findTenantOrThrow(String slug) {
        return tenantPersistencePort.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", slug));
    }
}
