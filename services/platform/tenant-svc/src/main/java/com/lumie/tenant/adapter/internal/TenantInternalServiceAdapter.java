package com.lumie.tenant.adapter.internal;

import com.lumie.internal.tenant.TenantInternalService;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.in.GetTenantUseCase;
import com.lumie.tenant.application.port.in.ValidateTenantUseCase;
import com.lumie.tenant.application.service.TenantRegistrationService;
import com.lumie.tenant.application.service.TenantRegistrationService.TenantRegistrationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInternalServiceAdapter implements TenantInternalService {

    private final GetTenantUseCase getTenantUseCase;
    private final ValidateTenantUseCase validateTenantUseCase;
    private final TenantRegistrationService tenantRegistrationService;

    @Override
    public Optional<TenantData> getTenant(Long tenantId) {
        try {
            TenantResponse tenant = getTenantUseCase.getTenantById(tenantId);
            return Optional.of(toData(tenant));
        } catch (Exception e) {
            log.warn("Tenant not found by id: {}", tenantId);
            return Optional.empty();
        }
    }

    @Override
    public Optional<TenantData> getTenantBySlug(String slug) {
        try {
            TenantResponse tenant = getTenantUseCase.getTenantBySlug(slug);
            return Optional.of(toData(tenant));
        } catch (Exception e) {
            log.warn("Tenant not found by slug: {}", slug);
            return Optional.empty();
        }
    }

    @Override
    public ValidateTenantResult validateTenant(String slug) {
        boolean isValid = validateTenantUseCase.isValidAndActive(slug);
        long tenantId = validateTenantUseCase.getTenantIdBySlug(slug).orElse(0L);
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
                    result.tenantId(), result.tenantSlug(), result.schemaName()
            );
        } catch (Exception e) {
            log.error("Failed to create tenant: {}", instituteName, e);
            return new CreateTenantResult(false, "Failed to create tenant: " + e.getMessage(),
                    null, null, null);
        }
    }

    private TenantData toData(TenantResponse tenant) {
        return new TenantData(
                tenant.id(), tenant.slug(), tenant.name(),
                tenant.status().name(), tenant.schemaName(),
                tenant.createdAt().toString(), tenant.updatedAt().toString()
        );
    }
}
