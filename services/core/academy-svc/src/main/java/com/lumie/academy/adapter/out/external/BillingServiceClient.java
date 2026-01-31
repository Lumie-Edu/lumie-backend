package com.lumie.academy.adapter.out.external;

import com.lumie.academy.application.port.out.BillingServicePort;
import com.lumie.grpc.billing.BillingServiceGrpc;
import com.lumie.grpc.billing.CheckQuotaRequest;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BillingServiceClient implements BillingServicePort {

    @GrpcClient("billing-svc")
    private BillingServiceGrpc.BillingServiceBlockingStub billingServiceStub;

    @Override
    public QuotaCheckResult checkQuota(String tenantSlug, String metricType) {
        try {
            var request = CheckQuotaRequest.newBuilder()
                    .setTenantSlug(tenantSlug)
                    .setMetricType(metricType)
                    .build();

            var response = billingServiceStub.checkQuota(request);

            return new QuotaCheckResult(
                response.getAllowed(),
                response.getCurrentUsage(),
                response.getLimit(),
                response.getMessage()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error checking quota for tenant: {}, metric: {}", tenantSlug, metricType, e);
            return new QuotaCheckResult(true, 0, Long.MAX_VALUE, "Quota check failed, allowing by default");
        }
    }
}
