package com.lumie.tenant.application.service;

import com.lumie.common.exception.DuplicateResourceException;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.application.dto.request.CreateTenantRequest;
import com.lumie.tenant.application.dto.request.UpdateTenantRequest;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.api.TenantCreatedEvent;
import com.lumie.tenant.application.port.out.SchemaProvisioningPort;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TenantCommandService {

    private final TenantPersistencePort tenantPersistencePort;
    private final SchemaProvisioningPort schemaProvisioningPort;
    private final ApplicationEventPublisher eventPublisher;

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

        // Free-subscription provisioning runs as an AFTER_COMMIT listener on this
        // event in billing. Breaks the previous tenant↔billing cycle
        // (CIRCULAR_MODULE_DEPS_AUDIT Resolution A, pair 3).
        eventPublisher.publishEvent(new TenantCreatedEvent(savedTenant.getId(), savedTenant.getSlugValue()));

        return TenantResponse.from(savedTenant);
    }

    public TenantResponse updateTenant(String slug, UpdateTenantRequest request) {
        log.info("Updating tenant with slug: {}", slug);

        Tenant tenant = findTenantOrThrow(slug);
        tenant.updateInfo(request.name(), request.displayName());
        tenant.updateContact(request.phone(), request.address());
        applyCustomIdUpdate(tenant, request.customId());
        Tenant updatedTenant = tenantPersistencePort.save(tenant);

        log.info("Tenant updated: {}", slug);
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
