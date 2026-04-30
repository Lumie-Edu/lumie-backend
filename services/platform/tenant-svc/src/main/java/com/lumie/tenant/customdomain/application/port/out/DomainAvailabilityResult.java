package com.lumie.tenant.customdomain.application.port.out;

public record DomainAvailabilityResult(
        boolean available,
        String registrationCost,
        String currency,
        String unavailableReason
) {
    public static DomainAvailabilityResult available(String cost, String currency) {
        return new DomainAvailabilityResult(true, cost, currency, null);
    }

    public static DomainAvailabilityResult unavailable(String reason) {
        return new DomainAvailabilityResult(false, null, null, reason);
    }
}
