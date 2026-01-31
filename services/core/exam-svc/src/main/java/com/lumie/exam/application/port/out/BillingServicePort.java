package com.lumie.exam.application.port.out;

public interface BillingServicePort {

    boolean hasOmrQuota();

    void consumeOmrQuota();

    OmrQuotaInfo getOmrQuotaInfo();

    record OmrQuotaInfo(
            int used,
            int limit,
            int remaining
    ) {
    }
}
