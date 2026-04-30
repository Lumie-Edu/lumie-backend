package com.lumie.tenant.customdomain.application.port.out;

public interface DomainAvailabilityPort {
    DomainAvailabilityResult check(String domain);
}
