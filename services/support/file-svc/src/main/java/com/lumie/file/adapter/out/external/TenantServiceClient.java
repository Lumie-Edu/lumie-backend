package com.lumie.file.adapter.out.external;

import com.lumie.file.application.port.out.TenantServicePort;
import com.lumie.grpc.tenant.GetTenantBySlugRequest;
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
    public boolean validateTenant(String slug) {
        try {
            var request = ValidateTenantRequest.newBuilder()
                    .setSlug(slug)
                    .build();

            var response = tenantServiceStub.validateTenant(request);
            return response.getValid();
        } catch (StatusRuntimeException e) {
            log.error("gRPC error validating tenant: {}", slug, e);
            return false;
        }
    }

    @Override
    public Optional<TenantData> getTenantBySlug(String slug) {
        try {
            var request = GetTenantBySlugRequest.newBuilder()
                    .setSlug(slug)
                    .build();

            var response = tenantServiceStub.getTenantBySlug(request);

            return Optional.of(new TenantData(
                response.getId(),
                response.getSlug(),
                response.getName(),
                response.getStatus()
            ));
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting tenant: {}", slug, e);
            return Optional.empty();
        }
    }
}
