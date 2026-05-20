package com.lumie.admin.application.dto.request;

public record AdminUpdateRequest(
    String name,
    String phone,
    String email,
    String adminMemo
) {
}
