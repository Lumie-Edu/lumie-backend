package com.lumie.billing.adapter.out.external;

import com.lumie.billing.application.port.out.TenantServicePort;
import com.lumie.grpc.tenant.GetTenantBySlugRequest;
import com.lumie.grpc.tenant.GetTenantRequest;
import com.lumie.grpc.tenant.TenantServiceGrpc;
import com.lumie.grpc.tenant.ValidateTenantRequest;
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
    public Optional<TenantData> getTenantBySlug(String slug) {
        log.debug("Getting tenant by slug: {}", slug);

        try {
            GetTenantBySlugRequest request = GetTenantBySlugRequest.newBuilder()
                    .setSlug(slug)
                    .build();

            com.lumie.grpc.tenant.TenantResponse response = tenantServiceStub.getTenantBySlug(request);

            return Optional.of(new TenantData(
                    response.getId(),
                    response.getSlug(),
                    response.getName(),
                    response.getStatus()
            ));
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting tenant by slug: {}", slug, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<TenantData> getTenantById(Long id) {
        log.debug("Getting tenant by id: {}", id);

        try {
            GetTenantRequest request = GetTenantRequest.newBuilder()
                    .setTenantId(id)
                    .build();

            com.lumie.grpc.tenant.TenantResponse response = tenantServiceStub.getTenant(request);

            return Optional.of(new TenantData(
                    response.getId(),
                    response.getSlug(),
                    response.getName(),
                    response.getStatus()
            ));
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting tenant by id: {}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean validateTenant(String slug) {
        log.debug("Validating tenant: {}", slug);

        try {
            ValidateTenantRequest request = ValidateTenantRequest.newBuilder()
                    .setSlug(slug)
                    .build();

            var response = tenantServiceStub.validateTenant(request);
            return response.getValid();
        } catch (StatusRuntimeException e) {
            log.error("gRPC error validating tenant: {}", slug, e);
            return false;
        }
    }
}
