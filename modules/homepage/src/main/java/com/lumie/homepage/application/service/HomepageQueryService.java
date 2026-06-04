package com.lumie.homepage.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.homepage.application.dto.response.HomepageConfigResponse;
import com.lumie.homepage.application.port.out.HomepageConfigPersistencePort;
import com.lumie.homepage.application.port.out.TenantLookupPort;
import com.lumie.homepage.application.port.out.TenantLookupPort.TenantInfo;
import com.lumie.homepage.domain.entity.HomepageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomepageQueryService {

    private final HomepageConfigPersistencePort homepageConfigPersistencePort;
    private final TenantLookupPort tenantLookupPort;
    private final ObjectMapper objectMapper;
    private final Tx tx;

    @Transactional(readOnly = true)
    public HomepageConfigResponse getCurrent() {
        return homepageConfigPersistencePort.find()
                .map(config -> HomepageConfigResponse.from(config, objectMapper))
                .orElseGet(() -> HomepageConfigResponse.from(HomepageConfig.defaultConfig(), objectMapper));
    }

    public Optional<HomepageConfigResponse> getPublicByCustomId(String customId) {
        Optional<TenantInfo> tenantInfo = tenantLookupPort.findByCustomId(customId);
        if (tenantInfo.isEmpty()) {
            log.debug("No tenant found for customId: {}", customId);
            return Optional.empty();
        }

        // This endpoint is public (no auth → no ambient tenant context), and the
        // tenant is only known after the customId lookup, so this method cannot
        // itself be @Transactional. Set slug + tenantId, then run the read on the
        // proxied @Transactional Tx bean: RlsTenantContextAspect binds
        // `app.tenant_id` to that transaction (slug alone leaves the row
        // invisible → public page falls back to the default homepage).
        TenantInfo info = tenantInfo.get();
        return TenantContextHolder.withinContext(info.slug(), info.tenantId(), tx::findCurrent);
    }

    /**
     * Owns the @Transactional read boundary for the public path. Extracted to a
     * separate proxied bean (mirrors {@code ExamJobTimeoutSweeper.Tx}) so the
     * call from {@link #getPublicByCustomId} crosses the Spring proxy — a
     * self-invocation would bypass the proxy and the RLS aspect would not fire.
     */
    @Component
    @RequiredArgsConstructor
    static class Tx {

        private final HomepageConfigPersistencePort homepageConfigPersistencePort;
        private final ObjectMapper objectMapper;

        @Transactional(readOnly = true)
        public Optional<HomepageConfigResponse> findCurrent() {
            // The homepage is always public once saved — no `published` gate. A
            // tenant that has never saved a config returns empty here, and the
            // public page renders the default landing template instead.
            return homepageConfigPersistencePort.find()
                    .map(config -> HomepageConfigResponse.from(config, objectMapper));
        }
    }
}
