package com.lumie.auth.adapter.out.external;

import com.lumie.auth.application.port.out.TenantServicePort;
import com.lumie.grpc.tenant.GetTenantBySlugRequest;
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
}
