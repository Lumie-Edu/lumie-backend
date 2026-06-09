package com.lumie.auth.api;

import com.lumie.event.DomainEvent;

public record StudentSelfRegisteredEvent(
        Long userId,
        String userLoginId,
        String name,
        String phone,
        String studentHighschool,
        Integer studentBirthYear,
        String parentPhone,
        String tenantSlug,
        Long tenantId
) implements DomainEvent {
}
