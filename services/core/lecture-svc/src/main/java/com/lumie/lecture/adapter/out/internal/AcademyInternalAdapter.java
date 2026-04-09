package com.lumie.lecture.adapter.out.internal;

import com.lumie.internal.academy.AcademyInternalService;
import com.lumie.lecture.application.port.out.AcademyServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcademyInternalAdapter implements AcademyServicePort {

    private final AcademyInternalService academyInternalService;

    @Override
    public boolean existsById(String tenantSlug, Long academyId) {
        return academyInternalService.getAcademy(tenantSlug, academyId).isPresent();
    }
}
