package com.lumie.student.application.dto;

public record StudentUpdateRequest(
    String name,
    String phone,
    String studentHighschool,
    Integer studentBirthYear,
    String parentPhone,
    String studentMemo
) {
}
