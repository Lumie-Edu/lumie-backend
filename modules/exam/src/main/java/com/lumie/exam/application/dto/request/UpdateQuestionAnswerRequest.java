package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateQuestionAnswerRequest(
        @NotBlank
        String selectedChoice
) {}
