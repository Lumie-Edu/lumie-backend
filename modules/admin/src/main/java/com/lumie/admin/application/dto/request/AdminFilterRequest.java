package com.lumie.admin.application.dto.request;

public record AdminFilterRequest(
    Boolean isActive,
    String search
) {
}
