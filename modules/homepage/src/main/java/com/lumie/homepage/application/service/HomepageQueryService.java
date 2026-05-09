package com.lumie.homepage.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.homepage.application.dto.response.HomepageConfigResponse;
import com.lumie.homepage.application.port.in.GetHomepageConfigUseCase;
import com.lumie.homepage.application.port.out.HomepageConfigPersistencePort;
import com.lumie.homepage.application.port.out.TenantLookupPort;
import com.lumie.homepage.application.port.out.TenantLookupPort.TenantInfo;
import com.lumie.homepage.domain.entity.HomepageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomepageQueryService implements GetHomepageConfigUseCase {

    private final HomepageConfigPersistencePort homepageConfigPersistencePort;
    private final TenantLookupPort tenantLookupPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<HomepageConfigResponse> getCurrent() {
        return homepageConfigPersistencePort.find()
                .map(config -> HomepageConfigResponse.from(config, objectMapper));
    }

    @Override
    public Optional<HomepageConfigResponse> getPublicByCustomId(String customId) {
        // Tenant lookup runs in public schema — its own transaction boundary is fine.
        Optional<TenantInfo> tenantInfo = tenantLookupPort.findByCustomId(customId);
        if (tenantInfo.isEmpty()) {
            log.debug("No tenant found for customId: {}", customId);
            return Optional.empty();
        }

        // Switch context BEFORE any persistence call so Hibernate opens a new
        // session (and picks the correct schema) after the switch.
        String previousTenant = TenantContextHolder.getTenant();
        try {
            TenantContextHolder.setTenant(tenantInfo.get().slug());
            // Spring Data JPA repository is @Transactional by default, so a new
            // transaction opens here — after the context switch.
            return homepageConfigPersistencePort.find()
                    .filter(HomepageConfig::isPublished)
                    .map(config -> HomepageConfigResponse.from(config, objectMapper));
        } finally {
            if (previousTenant != null) {
                TenantContextHolder.setTenant(previousTenant);
            } else {
                TenantContextHolder.clear();
            }
        }
    }
}
