package com.lumie.auth.adapter.out.external;

import com.lumie.auth.application.port.out.TenantServicePort;
import com.lumie.grpc.tenant.CreateTenantRequest;
import com.lumie.grpc.tenant.CreateTenantResponse;
import com.lumie.grpc.tenant.GetTenantBySlugRequest;
import com.lumie.grpc.tenant.GetTenantRequest;
import com.lumie.grpc.tenant.TenantResponse;
import com.lumie.grpc.tenant.TenantServiceGrpc;
import com.lumie.grpc.tenant.ValidateTenantRequest;
import com.lumie.grpc.tenant.ValidateTenantResponse;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class TenantServiceClient implements TenantServicePort {

    @GrpcClient("tenant-svc")
    private TenantServiceGrpc.TenantServiceBlockingStub tenantServiceStub;

    @Override
    public Optional<TenantData> validateTenant(String slug) {
        log.debug("Validating tenant via gRPC: {}", slug);

        try {
            ValidateTenantRequest request = ValidateTenantRequest.newBuilder()
                    .setSlug(slug)
                    .build();

            ValidateTenantResponse response = tenantServiceStub.validateTenant(request);

            if (!response.getValid()) {
                log.debug("Tenant validation failed: {}", response.getMessage());
                return Optional.empty();
            }

            // Get full tenant data
            return getTenantBySlug(slug);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error validating tenant: {}", slug, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<TenantData> getTenantBySlug(String slug) {
        log.debug("Getting tenant by slug via gRPC: {}", slug);

        try {
            GetTenantBySlugRequest request = GetTenantBySlugRequest.newBuilder()
                    .setSlug(slug)
                    .build();

            TenantResponse response = tenantServiceStub.getTenantBySlug(request);

            return Optional.of(new TenantData(
                    response.getId(),
                    response.getSlug(),
                    response.getName(),
                    response.getSchemaName(),
                    response.getStatus()
            ));
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting tenant: {}", slug, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<TenantData> validateTenantById(Long tenantId) {
        log.debug("Validating tenant by ID via gRPC: {}", tenantId);

        try {
            GetTenantRequest request = GetTenantRequest.newBuilder()
                    .setTenantId(tenantId)
                    .build();

            TenantResponse response = tenantServiceStub.getTenant(request);

            return Optional.of(new TenantData(
                    response.getId(),
                    response.getSlug(),
                    response.getName(),
                    response.getSchemaName(),
                    response.getStatus()
            ));
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting tenant by ID: {}", tenantId, e);
            return Optional.empty();
        }
    }

    @Override
    public TenantCreationResult createTenant(String instituteName, String businessRegistrationNumber,
                                              String ownerEmail, String ownerName) {
        log.debug("Creating tenant via gRPC: instituteName={}, ownerEmail={}", instituteName, ownerEmail);

        try {
            CreateTenantRequest request = CreateTenantRequest.newBuilder()
                    .setInstituteName(instituteName)
                    .setBusinessRegistrationNumber(businessRegistrationNumber)
                    .setOwnerEmail(ownerEmail)
                    .setOwnerName(ownerName)
                    .build();

            CreateTenantResponse response = tenantServiceStub.createTenant(request);

            return new TenantCreationResult(
                    response.getSuccess(),
                    response.getMessage(),
                    response.getTenantId(),
                    response.getTenantSlug(),
                    response.getSchemaName()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error creating tenant: {}", instituteName, e);
            return new TenantCreationResult(false, "gRPC error: " + e.getMessage(), null, null, null);
        }
    }
}
