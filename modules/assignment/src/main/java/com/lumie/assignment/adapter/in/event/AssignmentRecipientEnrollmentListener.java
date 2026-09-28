package com.lumie.assignment.adapter.in.event;

import com.lumie.assignment.application.service.AssignmentRecipientService;
import com.lumie.classroom.api.ClassEnrollmentCreatedEvent;
import com.lumie.common.tenant.TenantContextHolder;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class AssignmentRecipientEnrollmentListener {

    private final AssignmentRecipientService recipientService;

    @EventListener
    public void onEnrollmentCreated(ClassEnrollmentCreatedEvent event) {
        if (!Objects.equals(event.tenantId(), TenantContextHolder.getTenantId())) {
            return;
        }
        recipientService.addNewClassEnrollees(event.tenantSlug(), event.classId(), event.studentIds());
    }
}
