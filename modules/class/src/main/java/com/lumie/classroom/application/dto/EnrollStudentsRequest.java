package com.lumie.classroom.application.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EnrollStudentsRequest(
    @NotEmpty(message = "At least one student ID is required")
    List<Long> studentIds
) {
}
