package com.lumie.staff.application.dto.request;

public record StaffUpdateRequest(
    String name,
    String phone,
    String email,
    String staffMemo
) {
}
