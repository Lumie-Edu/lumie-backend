package com.lumie.tenant.customdomain.application.service;

import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.customdomain.application.dto.CustomDomainCheckResponse;
import com.lumie.tenant.customdomain.application.port.out.DomainAvailabilityPort;
import com.lumie.tenant.customdomain.application.port.out.DomainAvailabilityResult;
import com.lumie.tenant.domain.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomDomainCheckService {

    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.com$");

    private final TenantPersistencePort tenantPersistencePort;
    private final DomainAvailabilityPort domainAvailabilityPort;

    public CustomDomainCheckResponse check(String tenantSlug, String domain) {
        Tenant tenant = tenantPersistencePort.findBySlug(tenantSlug)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantSlug));

        if (!tenant.supportsCustomDomains()) {
            throw new IllegalStateException("Plan does not support custom domains: plan=" + tenant.getPlan());
        }

        String normalized = domain == null ? "" : domain.trim().toLowerCase(Locale.ROOT);
        if (!DOMAIN_PATTERN.matcher(normalized).matches()) {
            return new CustomDomainCheckResponse(normalized, false, null, null, "invalid_format");
        }

        DomainAvailabilityResult result = domainAvailabilityPort.check(normalized);
        return new CustomDomainCheckResponse(
                normalized,
                result.available(),
                result.registrationCost(),
                result.currency(),
                result.unavailableReason()
        );
    }
}
