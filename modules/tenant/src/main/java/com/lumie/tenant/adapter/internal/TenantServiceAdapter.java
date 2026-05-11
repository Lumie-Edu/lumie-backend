package com.lumie.tenant.adapter.internal;

import com.lumie.common.cache.CacheConfig;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.application.service.TenantQueryService;
import com.lumie.tenant.application.service.TenantRegistrationService;
import com.lumie.tenant.application.service.TenantRegistrationService.TenantRegistrationResult;
import com.lumie.tenant.api.TenantStatus;
import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.domain.vo.TenantPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantServiceAdapter implements TenantService {

    private final TenantQueryService tenantQueryService;
    private final TenantRegistrationService tenantRegistrationService;
    private final TenantPersistencePort tenantPersistencePort;

    @Override
    public Optional<TenantData> getTenant(Long tenantId) {
        try {
            TenantResponse tenant = tenantQueryService.getTenantById(tenantId);
            return Optional.of(toData(tenant));
        } catch (Exception e) {
            log.warn("Tenant not found by id: {}", tenantId);
            return Optional.empty();
        }
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_TENANTS_BY_SLUG, key = "#slug",
               unless = "#result == null || #result.isEmpty()")
    public Optional<TenantData> getTenantBySlug(String slug) {
        try {
            TenantResponse tenant = tenantQueryService.getTenantBySlug(slug);
            return Optional.of(toData(tenant));
        } catch (Exception e) {
            log.warn("Tenant not found by slug: {}", slug);
            return Optional.empty();
        }
    }

    @Override
    public Optional<TenantData> getTenantByCustomId(String customId) {
        return tenantPersistencePort.findByCustomId(customId)
                .filter(Tenant::isActive)
                .map(t -> new TenantData(
                        t.getId(), t.getSlugValue(), t.getName(),
                        t.getStatus(),
                        t.getCustomId(),
                        t.getCreatedAt() != null ? t.getCreatedAt().toString() : "",
                        t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : ""
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listActiveTenantSlugs() {
        return tenantPersistencePort.findAllByStatus(TenantStatus.ACTIVE).stream()
                .map(Tenant::getSlugValue)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantData> listActiveTenants() {
        return tenantPersistencePort.findAllByStatus(TenantStatus.ACTIVE).stream()
                .map(t -> new TenantData(
                        t.getId(), t.getSlugValue(), t.getName(),
                        t.getStatus(), t.getCustomId(),
                        t.getCreatedAt() != null ? t.getCreatedAt().toString() : "",
                        t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : ""
                ))
                .toList();
    }

    @Override
    public ValidateTenantResult validateTenant(String slug) {
        boolean isValid = tenantQueryService.isValidAndActive(slug);
        long tenantId = tenantQueryService.getTenantIdBySlug(slug).orElse(0L);
        return new ValidateTenantResult(
                isValid,
                isValid ? "Tenant is valid and active" : "Tenant is invalid or not active",
                tenantId
        );
    }

    @Override
    public CreateTenantResult createTenant(String instituteName, String businessRegistrationNumber,
                                           String ownerEmail, String ownerName) {
        try {
            TenantRegistrationResult result = tenantRegistrationService.registerTenant(
                    instituteName, businessRegistrationNumber, ownerEmail);
            return new CreateTenantResult(
                    result.success(), result.message(),
                    result.tenantId(), result.tenantSlug()
            );
        } catch (Exception e) {
            log.error("Failed to create tenant: {}", instituteName, e);
            return new CreateTenantResult(false, "Failed to create tenant: " + e.getMessage(),
                    null, null);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_TENANTS_BY_SLUG, key = "#tenantSlug")
    public void updatePlan(String tenantSlug, String planId) {
        TenantPlan plan = parsePlan(planId);
        if (plan == null) {
            log.warn("Unknown planId={} for tenant={}, skipping plan update", planId, tenantSlug);
            return;
        }
        Tenant tenant = tenantPersistencePort.findBySlug(tenantSlug)
                .orElse(null);
        if (tenant == null) {
            log.warn("Tenant not found for plan update: slug={}", tenantSlug);
            return;
        }
        tenant.updatePlan(plan);
        tenantPersistencePort.save(tenant);
        log.info("Tenant plan updated: slug={}, plan={}", tenantSlug, plan);
    }

    private TenantPlan parsePlan(String planId) {
        if (planId == null) return null;
        try {
            return TenantPlan.valueOf(planId.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TenantData toData(TenantResponse tenant) {
        return new TenantData(
                tenant.id(), tenant.slug(), tenant.name(),
                tenant.status(),
                tenant.customId(),
                tenant.createdAt().toString(), tenant.updatedAt().toString()
        );
    }
}
