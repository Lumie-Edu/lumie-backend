package com.lumie.admin.application.dto;

public record AdminFilterRequest(
    Long academyId,
    Boolean isActive,
    String search
) {
}
