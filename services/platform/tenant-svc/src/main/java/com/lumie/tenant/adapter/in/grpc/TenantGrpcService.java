package com.lumie.tenant.adapter.in.grpc;

import com.lumie.grpc.tenant.*;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.in.GetTenantUseCase;
import com.lumie.tenant.application.port.in.ValidateTenantUseCase;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TenantGrpcService extends TenantServiceGrpc.TenantServiceImplBase {

    private final GetTenantUseCase getTenantUseCase;
    private final ValidateTenantUseCase validateTenantUseCase;

    @Override
    public void getTenant(GetTenantRequest request, StreamObserver<com.lumie.grpc.tenant.TenantResponse> responseObserver) {
        log.debug("gRPC GetTenant request: id={}", request.getTenantId());

        try {
            TenantResponse tenant = getTenantUseCase.getTenantById(request.getTenantId());
            responseObserver.onNext(toGrpcResponse(tenant));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to get tenant by id: {}", request.getTenantId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getTenantBySlug(GetTenantBySlugRequest request, StreamObserver<com.lumie.grpc.tenant.TenantResponse> responseObserver) {
        log.debug("gRPC GetTenantBySlug request: slug={}", request.getSlug());

        try {
            TenantResponse tenant = getTenantUseCase.getTenantBySlug(request.getSlug());
            responseObserver.onNext(toGrpcResponse(tenant));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to get tenant by slug: {}", request.getSlug(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void validateTenant(ValidateTenantRequest request, StreamObserver<ValidateTenantResponse> responseObserver) {
        log.debug("gRPC ValidateTenant request: slug={}", request.getSlug());

        try {
            boolean isValid = validateTenantUseCase.isValidAndActive(request.getSlug());
            long tenantId = validateTenantUseCase.getTenantIdBySlug(request.getSlug()).orElse(0L);

            ValidateTenantResponse response = ValidateTenantResponse.newBuilder()
                    .setValid(isValid)
                    .setMessage(isValid ? "Tenant is valid and active" : "Tenant is invalid or not active")
                    .setTenantId(tenantId)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to validate tenant: {}", request.getSlug(), e);
            responseObserver.onError(e);
        }
    }

    private com.lumie.grpc.tenant.TenantResponse toGrpcResponse(TenantResponse tenant) {
        return com.lumie.grpc.tenant.TenantResponse.newBuilder()
                .setId(tenant.id())
                .setSlug(tenant.slug())
                .setName(tenant.name())
                .setStatus(tenant.status().name())
                .setSchemaName(tenant.schemaName())
                .setCreatedAt(tenant.createdAt().toString())
                .setUpdatedAt(tenant.updatedAt().toString())
                .build();
    }
}
