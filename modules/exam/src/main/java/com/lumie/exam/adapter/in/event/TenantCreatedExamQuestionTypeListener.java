package com.lumie.exam.adapter.in.event;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.service.ExamQuestionTypeService;
import com.lumie.tenant.api.TenantCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCreatedExamQuestionTypeListener {

    private final ExamQuestionTypeService examQuestionTypeService;

    @ApplicationModuleListener
    public void onTenantCreated(TenantCreatedEvent event) {
        TenantContextHolder.withinContext(event.tenantSlug(), event.tenantId(), () -> {
            examQuestionTypeService.seedDefaultQuestionTypesIfEmpty();
            return null;
        });
        log.info("Seeded exam question type defaults for tenant {}", event.tenantId());
    }
}
