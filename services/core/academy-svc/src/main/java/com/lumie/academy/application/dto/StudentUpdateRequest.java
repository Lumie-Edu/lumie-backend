package com.lumie.academy.application.dto;

public record StudentUpdateRequest(
    String name,
    String phone,
    String grade,
    String schoolName,
    String parentName,
    String parentPhone
) {
}
