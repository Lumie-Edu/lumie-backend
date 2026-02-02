package com.lumie.academy.application.dto;

public record StudentUpdateRequest(
    String name,
    String phone,
    String studentHighschool,
    Integer studentBirthYear,
    String studentMemo
) {
}
