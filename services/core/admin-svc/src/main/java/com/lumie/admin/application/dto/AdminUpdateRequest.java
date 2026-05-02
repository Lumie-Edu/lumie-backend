package com.lumie.admin.application.dto;

public record AdminUpdateRequest(
    String name,
    String phone,
    String email,
    String adminMemo
) {
}
