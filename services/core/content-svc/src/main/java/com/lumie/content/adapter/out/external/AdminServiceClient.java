package com.lumie.content.adapter.out.external;

import com.lumie.content.application.port.out.AdminServicePort;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class AdminServiceClient implements AdminServicePort {

    @Override
    public boolean validateAdmin(String tenantSlug, Long adminId) {
        // TODO: Implement when admin gRPC service is available in academy-svc
        // For now, return true as admin validation will be handled by auth-svc token
        log.debug("Admin validation for adminId: {} in tenant: {} - returning true (placeholder)", adminId, tenantSlug);
        return true;
    }

    @Override
    public Optional<AdminData> getAdmin(String tenantSlug, Long adminId) {
        // TODO: Implement when admin gRPC service is available in academy-svc
        // For now, return empty as admin data should come from auth context
        log.debug("Get admin for adminId: {} in tenant: {} - returning empty (placeholder)", adminId, tenantSlug);
        return Optional.empty();
    }
}
