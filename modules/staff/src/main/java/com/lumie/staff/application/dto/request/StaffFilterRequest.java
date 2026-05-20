package com.lumie.staff.application.dto.request;

public record StaffFilterRequest(
    Boolean isActive,
    String search
) {
}
