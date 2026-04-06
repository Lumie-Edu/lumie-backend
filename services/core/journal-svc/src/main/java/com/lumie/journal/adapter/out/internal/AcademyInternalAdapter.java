package com.lumie.journal.adapter.out.internal;

import com.lumie.journal.application.port.out.AcademyServicePort;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.academy.AcademyInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("journalAcademyInternalAdapter")
@RequiredArgsConstructor
public class AcademyInternalAdapter implements AcademyServicePort {

    private final AcademyInternalService academyInternalService;

    @Override
    public Optional<AcademyInfo> getAcademy(Long academyId) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Getting academy via internal API: {} in tenant: {}", academyId, slug);
        return academyInternalService.getAcademy(slug, academyId)
                .map(a -> new AcademyInfo(a.id(), a.academyName(), a.isActive()));
    }
}
