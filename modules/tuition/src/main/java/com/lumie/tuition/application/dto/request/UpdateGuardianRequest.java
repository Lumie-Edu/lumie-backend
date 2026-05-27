package com.lumie.tuition.application.dto.request;

public record UpdateGuardianRequest(
        String name,
        String phone,
        String email,
        String relationship
) {}
