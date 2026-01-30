package com.lumie.billing.adapter.in.grpc;

import com.lumie.billing.application.port.in.CheckQuotaUseCase;
import com.lumie.billing.application.port.in.GetPlansUseCase;
import com.lumie.billing.application.port.in.GetSubscriptionUseCase;
import com.lumie.billing.domain.vo.MetricType;
import com.lumie.billing.domain.vo.PlanLimits;
import com.lumie.grpc.billing.BillingServiceGrpc;
import com.lumie.grpc.billing.CheckQuotaRequest;
import com.lumie.grpc.billing.CheckQuotaResponse;
import com.lumie.grpc.billing.GetPlanFeaturesRequest;
import com.lumie.grpc.billing.GetSubscriptionRequest;
import com.lumie.grpc.billing.PlanFeaturesResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class BillingGrpcService extends BillingServiceGrpc.BillingServiceImplBase {

    private final GetSubscriptionUseCase getSubscriptionUseCase;
    private final CheckQuotaUseCase checkQuotaUseCase;
    private final GetPlansUseCase getPlansUseCase;

    @Override
    public void getSubscription(GetSubscriptionRequest request,
                                 StreamObserver<com.lumie.grpc.billing.SubscriptionResponse> responseObserver) {
        log.debug("gRPC GetSubscription request: tenantSlug={}", request.getTenantSlug());

        try {
            com.lumie.billing.application.dto.response.SubscriptionResponse subscription =
                    getSubscriptionUseCase.getSubscription(request.getTenantSlug());

            responseObserver.onNext(toGrpcSubscriptionResponse(subscription));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to get subscription for tenant: {}", request.getTenantSlug(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void checkQuota(CheckQuotaRequest request,
                           StreamObserver<CheckQuotaResponse> responseObserver) {
        log.debug("gRPC CheckQuota request: tenantSlug={}, metricType={}",
                request.getTenantSlug(), request.getMetricType());

        try {
            MetricType metricType = MetricType.valueOf(request.getMetricType());
            com.lumie.billing.application.dto.response.QuotaCheckResponse quotaCheck =
                    checkQuotaUseCase.checkQuota(request.getTenantSlug(), metricType);

            responseObserver.onNext(toGrpcCheckQuotaResponse(quotaCheck));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to check quota for tenant: {}", request.getTenantSlug(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getPlanFeatures(GetPlanFeaturesRequest request,
                                 StreamObserver<PlanFeaturesResponse> responseObserver) {
        log.debug("gRPC GetPlanFeatures request: planId={}", request.getPlanId());

        try {
            com.lumie.billing.application.dto.response.PlanResponse plan =
                    getPlansUseCase.getPlanById(request.getPlanId());

            responseObserver.onNext(toGrpcPlanFeaturesResponse(plan));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to get plan features: {}", request.getPlanId(), e);
            responseObserver.onError(e);
        }
    }

    private com.lumie.grpc.billing.SubscriptionResponse toGrpcSubscriptionResponse(
            com.lumie.billing.application.dto.response.SubscriptionResponse subscription) {
        com.lumie.grpc.billing.SubscriptionResponse.Builder builder =
                com.lumie.grpc.billing.SubscriptionResponse.newBuilder()
                        .setId(subscription.id())
                        .setTenantId(subscription.tenantId())
                        .setTenantSlug(subscription.tenantSlug())
                        .setPlanId(subscription.planId())
                        .setPlanName(subscription.planName())
                        .setStatus(subscription.status().name())
                        .setStartedAt(subscription.startedAt().toString())
                        .setLimits(toGrpcPlanLimits(subscription.limits()));

        if (subscription.expiresAt() != null) {
            builder.setExpiresAt(subscription.expiresAt().toString());
        }

        return builder.build();
    }

    private CheckQuotaResponse toGrpcCheckQuotaResponse(
            com.lumie.billing.application.dto.response.QuotaCheckResponse quotaCheck) {
        return CheckQuotaResponse.newBuilder()
                .setAllowed(quotaCheck.allowed())
                .setCurrentUsage(quotaCheck.currentUsage())
                .setLimit(quotaCheck.limit())
                .setMetricType(quotaCheck.metricType().name())
                .setMessage(quotaCheck.message())
                .build();
    }

    private PlanFeaturesResponse toGrpcPlanFeaturesResponse(
            com.lumie.billing.application.dto.response.PlanResponse plan) {
        PlanFeaturesResponse.Builder builder = PlanFeaturesResponse.newBuilder()
                .setPlanId(plan.id())
                .setPlanName(plan.name())
                .setLimits(toGrpcPlanLimits(plan.limits()));

        if (plan.features().customDomains()) {
            builder.addFeatures("custom_domains");
        }
        if (plan.features().advancedAnalytics()) {
            builder.addFeatures("advanced_analytics");
        }
        if (plan.features().prioritySupport()) {
            builder.addFeatures("priority_support");
        }
        if (plan.features().apiAccess()) {
            builder.addFeatures("api_access");
        }
        if (plan.features().whiteLabeling()) {
            builder.addFeatures("white_labeling");
        }
        plan.features().additionalFeatures().forEach(builder::addFeatures);

        return builder.build();
    }

    private com.lumie.grpc.billing.PlanLimits toGrpcPlanLimits(PlanLimits limits) {
        return com.lumie.grpc.billing.PlanLimits.newBuilder()
                .setMaxStudents(limits.maxStudents())
                .setMaxAcademies(limits.maxAcademies())
                .setMaxAdmins(limits.maxAdmins())
                .setOmrMonthlyQuota(limits.omrMonthlyQuota())
                .build();
    }
}
