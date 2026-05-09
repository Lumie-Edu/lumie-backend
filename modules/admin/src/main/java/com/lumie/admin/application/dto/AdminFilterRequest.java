package com.lumie.admin.application.dto;

public record AdminFilterRequest(
    Boolean isActive,
    String search
) {
}
