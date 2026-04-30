package com.lumie.tenant.customdomain.application.dto;

public record CustomDomainCheckResponse(
        String domain,
        boolean available,
        String registrationCost,
        String currency,
        String unavailableReason
) {}
