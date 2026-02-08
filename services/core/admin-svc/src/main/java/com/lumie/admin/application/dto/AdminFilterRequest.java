package com.lumie.admin.application.dto;

public record AdminFilterRequest(
    Long positionId,
    Long academyId,
    Boolean isActive,
    String search
) {
}
