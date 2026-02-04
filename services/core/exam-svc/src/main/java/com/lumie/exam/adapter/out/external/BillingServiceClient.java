package com.lumie.exam.adapter.out.external;

import com.lumie.exam.application.port.out.BillingServicePort;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.grpc.billing.BillingServiceGrpc;
import com.lumie.grpc.billing.CheckQuotaRequest;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BillingServiceClient implements BillingServicePort {

    private static final String OMR_METRIC_TYPE = "OMR_GRADING";

    @GrpcClient("billing-svc")
    private BillingServiceGrpc.BillingServiceBlockingStub billingServiceStub;

    @Override
    public boolean hasOmrQuota() {
        try {
            var request = CheckQuotaRequest.newBuilder()
                    .setTenantSlug(TenantContextHolder.getRequiredTenant())
                    .setMetricType(OMR_METRIC_TYPE)
                    .build();

            var response = billingServiceStub.checkQuota(request);
            return response.getAllowed();
        } catch (StatusRuntimeException e) {
            log.error("gRPC error checking OMR quota", e);
            return false;
        }
    }

    @Override
    public void consumeOmrQuota() {
        // Quota consumption is handled by billing-svc when recording usage
        // This is a no-op as the actual consumption happens via event or direct call
        log.debug("OMR quota consumption recorded");
    }

    @Override
    public OmrQuotaInfo getOmrQuotaInfo() {
        try {
            var request = CheckQuotaRequest.newBuilder()
                    .setTenantSlug(TenantContextHolder.getRequiredTenant())
                    .setMetricType(OMR_METRIC_TYPE)
                    .build();

            var response = billingServiceStub.checkQuota(request);
            int limit = (int) response.getLimit();
            int used = (int) response.getCurrentUsage();
            return new OmrQuotaInfo(used, limit, limit - used);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting OMR quota info", e);
            return new OmrQuotaInfo(0, 0, 0);
        }
    }
}
