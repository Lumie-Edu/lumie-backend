package com.lumie.tuition.application.dto.response;

import com.lumie.tuition.domain.entity.Guardian;

import java.time.Instant;

public record GuardianResponse(
        Long id,
        String name,
        String phone,
        String email,
        String relationship,
        Instant createdAt,
        Instant updatedAt
) {
    public static GuardianResponse from(Guardian guardian) {
        return new GuardianResponse(
                guardian.getId(),
                guardian.getName(),
                guardian.getPhone(),
                guardian.getEmail(),
                guardian.getRelationship(),
                guardian.getCreatedAt(),
                guardian.getUpdatedAt()
        );
    }
}
