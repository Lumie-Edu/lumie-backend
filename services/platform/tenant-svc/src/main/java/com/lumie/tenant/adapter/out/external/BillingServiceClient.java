package com.lumie.tenant.adapter.out.external;

import com.lumie.grpc.billing.BillingServiceGrpc;
import com.lumie.grpc.billing.CreateFreeSubscriptionRequest;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BillingServiceClient {

    @GrpcClient("billing-svc")
    private BillingServiceGrpc.BillingServiceBlockingStub billingServiceStub;

    public void createFreeSubscription(Long tenantId, String tenantSlug) {
        log.debug("Creating free subscription for tenant: {}", tenantSlug);

        try {
            CreateFreeSubscriptionRequest request = CreateFreeSubscriptionRequest.newBuilder()
                    .setTenantId(tenantId)
                    .setTenantSlug(tenantSlug)
                    .build();

            billingServiceStub.createFreeSubscription(request);
            log.info("Free subscription created for tenant: {}", tenantSlug);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error creating free subscription for tenant: {}", tenantSlug, e);
            throw new RuntimeException("Failed to create free subscription: " + e.getMessage(), e);
        }
    }
}
