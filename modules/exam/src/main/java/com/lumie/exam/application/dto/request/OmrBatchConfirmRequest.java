package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OmrBatchConfirmRequest(
        @NotBlank String batchKey,
        @NotNull @Size(min = 1, max = 100)
        List<String> objectKeys
) {}
