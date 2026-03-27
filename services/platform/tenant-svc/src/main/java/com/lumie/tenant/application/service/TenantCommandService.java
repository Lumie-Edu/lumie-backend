package com.lumie.tenant.application.service;

import com.lumie.common.exception.DuplicateResourceException;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.adapter.out.external.BillingServiceClient;
import com.lumie.tenant.application.dto.request.CreateTenantRequest;
import com.lumie.tenant.application.dto.request.UpdateTenantRequest;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.in.CreateTenantUseCase;
import com.lumie.tenant.application.port.in.DeleteTenantUseCase;
import com.lumie.tenant.application.port.in.SuspendTenantUseCase;
import com.lumie.tenant.application.port.in.UpdateTenantUseCase;
import com.lumie.tenant.application.port.out.SchemaProvisioningPort;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TenantCommandService implements
        CreateTenantUseCase,
        UpdateTenantUseCase,
        DeleteTenantUseCase,
        SuspendTenantUseCase {

    private final TenantPersistencePort tenantPersistencePort;
    private final SchemaProvisioningPort schemaProvisioningPort;
    private final BillingServiceClient billingServiceClient;

    @Override
    public TenantResponse createTenant(CreateTenantRequest request) {
        log.info("Creating tenant with slug: {}", request.slug());

        if (tenantPersistencePort.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Tenant", request.slug());
        }

        Tenant tenant = Tenant.create(
                request.slug(),
                request.name(),
                request.displayName(),
                request.ownerEmail()
        );

        Tenant savedTenant = tenantPersistencePort.save(tenant);
        log.info("Tenant created with ID: {}, slug: {}", savedTenant.getId(), savedTenant.getSlugValue());

        savedTenant.startProvisioning();
        tenantPersistencePort.save(savedTenant);

        String schemaName = savedTenant.getSchemaName();
        if (!schemaProvisioningPort.schemaExists(schemaName)) {
            schemaProvisioningPort.createSchema(schemaName);
            log.info("Schema created: {}", schemaName);
        }
        schemaProvisioningPort.migrateSchema(schemaName);
        log.info("Schema migration completed: {}", schemaName);

        savedTenant.activate();
        tenantPersistencePort.save(savedTenant);

        billingServiceClient.createFreeSubscription(savedTenant.getId(), savedTenant.getSlugValue());

        return TenantResponse.from(savedTenant);
    }

    @Override
    public TenantResponse updateTenant(String slug, UpdateTenantRequest request) {
        log.info("Updating tenant with slug: {}", slug);

        Tenant tenant = findTenantOrThrow(slug);
        tenant.updateInfo(request.name(), request.displayName());
        Tenant updatedTenant = tenantPersistencePort.save(tenant);

        log.info("Tenant updated: {}", slug);
        return TenantResponse.from(updatedTenant);
    }

    @Override
    public void deleteTenant(String slug) {
        log.info("Deleting tenant with slug: {}", slug);

        Tenant tenant = findTenantOrThrow(slug);
        tenant.markAsDeleted();
        tenantPersistencePort.save(tenant);

        log.info("Tenant marked as deleted: {}", slug);
    }

    @Override
    public TenantResponse suspendTenant(String slug, String reason) {
        log.info("Suspending tenant: {} with reason: {}", slug, reason);

        Tenant tenant = findTenantOrThrow(slug);
        tenant.suspend();
        Tenant savedTenant = tenantPersistencePort.save(tenant);

        return TenantResponse.from(savedTenant);
    }

    @Override
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
