package com.lumie.assignment.adapter.out.internal;

import com.lumie.assignment.application.port.out.AcademyServicePort;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.academy.AcademyInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("assignmentAcademyInternalAdapter")
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
